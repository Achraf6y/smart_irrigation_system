this folder contains two versions of the code but here is the main difference

  - First, obviously one is coded in java and the other in python, for receiving data sent by the HC-05 bluetooth module
and inserting them in the Derby (javaDB) database, both codes works fine and in a similar way, (note that you should add the class paths in the java
code, if you are using terminal, use `javac -cp firstfile.jar:secondefile.jar sensor_data_insertion.java` & 
                                     `java -cp firstfile.jar:secondefile.jar:. sensor_data_insertion`
we only used the python code so that we can upload the data into firebase realtime database at the same time that
its inserted in the Derby database, i know it sounds like a duplicated work, but keep in mind that this project was meant to use
derby database and the strenghts that it offers in embedded systems, and the choice of firebase was for the simple reason that 
its simply used with flutter.
