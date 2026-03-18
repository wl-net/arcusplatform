#!/bin/bash
#
# Generates TLS certificates for local hub-bridge <-> hub-agent development.
#
# Creates:
#   $OUT_DIR/ca.crt                - Self-signed CA certificate
#   $OUT_DIR/hub-bridge-keystore.jks   - Hub-bridge server keystore
#   $OUT_DIR/hub-bridge-truststore.jks - Hub-bridge truststore (trusts hub certs)
#   /tmp/hub.crt                   - Hub client certificate (read by simulated agent)
#   /tmp/hub.key                   - Hub client private key (read by simulated agent)
#
# Usage:
#   ./tools/setup-local-tls.sh [HUB_MAC]
#
# HUB_MAC defaults to 00:11:22:33:44:55 (the simulated agent's MAC).

set -euo pipefail

HUB_MAC="${1:-00:11:22:33:44:55}"
HUB_MAC=$(echo "$HUB_MAC" | tr '[:lower:]' '[:upper:]')
HUB_CN="ih200-${HUB_MAC}"
OUT_DIR="${HOME}/.arcus/tls"
PASS="arcusdev"

mkdir -p "$OUT_DIR"

echo "=== Generating local TLS certs ==="
echo "  Hub CN:   ${HUB_CN}"
echo "  Out dir:  ${OUT_DIR}"
echo ""

# --- CA ---
if [ ! -f "$OUT_DIR/ca.key" ]; then
   echo "--- Creating CA ---"
   openssl req -x509 -newkey rsa:2048 -nodes \
      -keyout "$OUT_DIR/ca.key" \
      -out "$OUT_DIR/ca.crt" \
      -days 3650 \
      -subj "/CN=Arcus Local Dev CA"
else
   echo "--- CA already exists, reusing ---"
fi

# --- Hub-bridge server cert ---
echo "--- Creating hub-bridge server certificate ---"
openssl req -newkey rsa:2048 -nodes \
   -keyout "$OUT_DIR/hub-bridge-server.key" \
   -out "$OUT_DIR/hub-bridge-server.csr" \
   -subj "/CN=localhost"

openssl x509 -req \
   -in "$OUT_DIR/hub-bridge-server.csr" \
   -CA "$OUT_DIR/ca.crt" \
   -CAkey "$OUT_DIR/ca.key" \
   -CAcreateserial \
   -out "$OUT_DIR/hub-bridge-server.crt" \
   -days 3650 \
   -extfile <(printf "subjectAltName=DNS:localhost,IP:127.0.0.1")

# --- Hub client cert ---
echo "--- Creating hub client certificate (CN=${HUB_CN}) ---"
openssl req -newkey rsa:2048 -nodes \
   -keyout "$OUT_DIR/hub.key" \
   -out "$OUT_DIR/hub.csr" \
   -subj "/CN=${HUB_CN}"

openssl x509 -req \
   -in "$OUT_DIR/hub.csr" \
   -CA "$OUT_DIR/ca.crt" \
   -CAkey "$OUT_DIR/ca.key" \
   -CAcreateserial \
   -out "$OUT_DIR/hub.crt" \
   -days 3650

# --- Hub-bridge server keystore (PKCS12 -> JKS) ---
echo "--- Creating hub-bridge keystore ---"
rm -f "$OUT_DIR/hub-bridge-keystore.jks"
openssl pkcs12 -export \
   -in "$OUT_DIR/hub-bridge-server.crt" \
   -inkey "$OUT_DIR/hub-bridge-server.key" \
   -certfile "$OUT_DIR/ca.crt" \
   -name "hub-bridge" \
   -out "$OUT_DIR/hub-bridge-keystore.p12" \
   -password "pass:${PASS}"

keytool -importkeystore \
   -srckeystore "$OUT_DIR/hub-bridge-keystore.p12" \
   -srcstoretype PKCS12 \
   -srcstorepass "$PASS" \
   -destkeystore "$OUT_DIR/hub-bridge-keystore.jks" \
   -deststoretype JKS \
   -deststorepass "$PASS" \
   -noprompt 2>/dev/null

# --- Hub-bridge truststore (trusts the CA that signed hub certs) ---
echo "--- Creating hub-bridge truststore ---"
rm -f "$OUT_DIR/hub-bridge-truststore.jks"
keytool -importcert \
   -file "$OUT_DIR/ca.crt" \
   -alias "arcus-local-ca" \
   -keystore "$OUT_DIR/hub-bridge-truststore.jks" \
   -storepass "$PASS" \
   -noprompt 2>/dev/null

# --- Install hub cert/key where the agent expects them ---
echo "--- Installing hub cert/key ---"
MFG_DIR="${HOME}/.hub-simulated/tmp/mfg"
mkdir -p "$MFG_DIR/certs" "$MFG_DIR/keys"
cp "$OUT_DIR/hub.crt" "$MFG_DIR/certs/${HUB_MAC}.crt"
# Convert key to PKCS8 PEM (what SslKeyStore.readHubPrivateKey expects)
openssl pkcs8 -topk8 -nocrypt \
   -in "$OUT_DIR/hub.key" \
   -out "$MFG_DIR/keys/${HUB_MAC}.key"
echo "  Installed to ${MFG_DIR}/certs/${HUB_MAC}.crt"

# --- Create agent truststore (copy of original + local CA) ---
# The agent loads truststore.jks from the system classpath. We create a merged
# copy at $OUT_DIR/truststore.jks so that adding $OUT_DIR to the classpath
# (before the default resources) makes the agent trust our local CA without
# modifying the checked-in truststore.
echo "--- Creating local agent truststore ---"
AGENT_TRUSTSTORE="$(dirname "$0")/../agent/arcus-system/src/main/resources/truststore.jks"
AGENT_TS_PASS="8EFJhxm7aRs2hmmKwVuM9RPSwhNCtMpC"
LOCAL_AGENT_TS="$OUT_DIR/truststore.jks"
rm -f "$LOCAL_AGENT_TS"
if [ -f "$AGENT_TRUSTSTORE" ]; then
   cp "$AGENT_TRUSTSTORE" "$LOCAL_AGENT_TS"
   keytool -importcert \
      -file "$OUT_DIR/ca.crt" \
      -alias "arcus-local-ca" \
      -keystore "$LOCAL_AGENT_TS" \
      -storepass "$AGENT_TS_PASS" \
      -noprompt 2>/dev/null
   echo "  Created ${LOCAL_AGENT_TS}"
else
   echo "  WARNING: Agent truststore not found at ${AGENT_TRUSTSTORE}"
fi

# --- Clean up CSRs ---
rm -f "$OUT_DIR"/*.csr "$OUT_DIR"/*.srl "$OUT_DIR/hub-bridge-keystore.p12"

echo ""
echo "=== Done ==="
echo ""
echo "Hub-bridge properties (add to your local properties file):"
echo ""
echo "tls.server=true"
echo "tls.need.client.auth=true"
echo "tls.server.keystore.filepath=${OUT_DIR}/hub-bridge-keystore.jks"
echo "tls.server.keystore.password=${PASS}"
echo "tls.server.key.password=${PASS}"
echo "tls.server.truststore.filepath=${OUT_DIR}/hub-bridge-truststore.jks"
echo "tls.server.truststore.password=${PASS}"
echo "use.ssl=true"
echo ""
echo "Agent: set IRIS_AGENT_CLASSPATH before running:"
echo ""
echo "export IRIS_AGENT_CLASSPATH=${OUT_DIR}"
