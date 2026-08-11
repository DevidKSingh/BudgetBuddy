@echo off
if not exist bin mkdir bin
javac -d bin -sourcepath src src/com/expensetracker/Main.java src/com/expensetracker/model/*.java src/com/expensetracker/storage/*.java src/com/expensetracker/service/*.java src/com/expensetracker/ui/*.java src/com/expensetracker/cli/*.java
java -cp bin com.expensetracker.Main --cli
pause
