# Google Closure Compiler Maven Plugin

This project is forked from https://github.com/gli/closure-compiler-maven-plugin
to implement additional features.


### Entry point configuration

Configure the plugin to include only transitive dependencies of specified entry
points.

    <entryPoints>
      <entryPoint>application.Main</entryPoint>
    </entryPoints>


### goog.require returns namespace

Patched Closure Compiler so `goog.require` can appear inside `goog.scope`, and
`goog.require` returns the namespace so you can assign it to a variable.  For
example:

    goog.provide('some.Example');

    goog.scope(function() {
      var Vec2 = goog.require('goog.math.Vec2');

      some.Example = function() {
        console.log(new Vec2());
      };
    });

See https://code.google.com/p/closure-compiler/issues/detail?id=674


## Configure Your Maven Project To Use Plugin

    <properties>
      <closure.source>src/main/js</closure.source>
      <closure.externs>src/main/externs</closure.externs>
      <closure.outputFile>${project.build.directory}/compiled.js</closure.outputFile>
    </properties>

    <build>
      <plugins>
        <!-- ... -->

        <plugin>
          <groupId>com.github.pukkaone</groupId>
          <artifactId>closure-compiler-maven-plugin</artifactId>
          <version>1.0.0</version>
          <executions>
            <execution>
              <phase>process-resources</phase>
              <goals>
                <goal>compile</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <sourceDirectory>${closure.source}</sourceDirectory>
            <externsSourceDirectory>${closure.externs}</externsSourceDirectory>
            <entryPoints>
              <entryPoint>application.Main</entryPoint>
            </entryPoints>
            <outputFile>${closure.outputFile}</outputFile>
            <compilationLevel>SIMPLE_OPTIMIZATIONS</compilationLevel>
            <merge>true</merge>
            <loggingLevel>WARNING</loggingLevel>
            <warningLevel>VERBOSE</warningLevel>
            <generateExports>true</generateExports>
            <addDefaultExterns>true</addDefaultExterns>
            <logExternFiles>true</logExternFiles>
            <logSourceFiles>true</logSourceFiles>
            <stopOnErrors>true</stopOnErrors>
            <stopOnWarnings>true</stopOnWarnings>
          </configuration>
        </plugin>

        <!-- ... -->
      </plugins>
    </build>
