package com.arcussmarthome.protoc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import org.gradle.api.file.FileCollection

class IrisProtocTask extends DefaultTask {
   def packageName
   def source
   def destination
   def generateBindings = false

   FileCollection getSource() {
      if (source == null) {
          return project.files([])
      }

      project.files(source)
   }

   File getMainDestination() {
      if (destination == null) {
         return project.file("$project.buildDir/generated-src/main/java")
      }

      project.file(destination + "/test/java")
   }

   File getTestDestination() {
      if (destination == null) {
         return project.file("$project.buildDir/generated-src/test/java")
      }

      project.file(destination + "/main/java")
   }

   @TaskAction
   def compile() {
      def src = getSource()
      def mainDst = getMainDestination()
      def testDst = getTestDestination()

      mainDst.parentFile.mkdirs()
      testDst.parentFile.mkdirs()

      ProtocGeneratorOptions options = new ProtocGeneratorOptions(mainDst.getPath(), packageName, testDst.getPath(), generateBindings);
      IrisProtoCompiler compiler = new IrisProtoCompiler(src.getFiles(), options);
      compiler.run();
   }
}
