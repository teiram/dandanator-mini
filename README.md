# dandanator-mini
ROM assembler for the [Dandanator Mini](http://www.dandare.es/Proyectos_Dandare/ZX_Dandanator!_Mini.html). A Spectrum ZX peripheral with many features.


##Requisites
Git, Maven and java8 are needed

##Cloning the repository
	git clone https://github.com/teiram/dandanator-mini.git
	
##Building
Java8 and Maven are needed. Just execute:

	cd dandanator-mini
	mvn install
	
###Generating native packages. 
Native packages are supported by means of the [javafx maven plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin).
They can be generated by executing:

    mvn jfx:native
    
They can be found under the directory:

    target/jfx/native
    
The plugin is configured to not include the java runtime. This is to avoid very big deliverables, since with java8 the complete JRE 
would be included.

The linux native executable won't work if the variable JRE_HOME is not defined pointing to a java8 including the JFX classes. 
This means that probably an OpenJDK VM won't work and you would need a Oracle JVM or to provide the JavaFX jars yourself.

##Executing

A jar with all the dependencies is generated and can be executed directly with the following invocation:

	java -jar target/dandanator-mini-5.0-jar-with-dependencies.jar
	
In case you've generated a native package, just execute the binary you'll find in the application folder (or the generated app in OSX)
 
