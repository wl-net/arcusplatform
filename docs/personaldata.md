# Personal Data
This page explains what data Arcus collects and how it is protected.

## Where is data stored?

Long-lived data (e.g. storage beyond a week) is stored in cassandra. Short-lived data (e.g. messages in transit, etc.) is "stored" in kafka topics.

## Use Cases

1. Phone number is used to make calls when alarms go off.
1. Email address is used to send notification emails.
1. 4 digit pin codes are used for alarm disarming.
1. 4 digit pin codes are used by locks to allow access to doors. 

## User/Account Information

The following data is collected 

| Datatype | Use Cases | Protection | Retention |
|---|---|---|---|
| First Name |  | Stored plaintext in cassandra | ? |
| Last Name | | Stored plaintext in cassandra | ? |
| Phone number | #1 |Stored plaintext in cassandra | ? |
| Email address | #2 | Stored plaintext in cassandra | ? |
| Mobile number verification | ? | Stored plaintext in cassandra | ? |
| pin codes | #3, #4 | Stored encrypted in cassandra, stored plaintext on hub? | ? |
| Security question answers | ? (probably stop collecting this) | stored as map of question -> answer. answer to questions is encrypted at rest. information about which questions answered is not | ? |
| User IP address | ? | ? | ? |
| Password | ? | Hashed with salted SHA256 1000 rounds (as build by apache shiro authentication) | ? |
| Session id | ? | Stored plaintext in cassandra | 30 days? |


## Device Information

The following data is collected 

| Datatype | Use Cases | Protection | Retention |
|---|---|---|---|
| ZWave Network Key | | |
| Zigbee Network Key | | |
| Hub MAC address | | |
| Hub external IP address | | |
| Hub internal IP address | | |
| Hub last IP address | | |
| Hub last contact time | | |

## Cryptography

Arcus originally shipped with AES in CBC mode with a fixed IV. This has several weaknesses which are being addressed by migrating to AES-GCM and implementing regular online key rotation.

in-transit encryption between the user and service and hub and service is provided by TLS 1.2 with modern (e.g. AEAD) ciphers. Internally, Istio is used to provide service to service authentication and encryption in transit for kubernetes (e.g. arcus-k8) deployments.