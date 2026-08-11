@echo off
echo Building Daily Expense Tracker System...
if not exist bin mkdir bin
javac -d bin -sourcepath src src/com/expensetracker/Main.java src/com/expensetracker/model/*.java src/com/expensetracker/storage/*.java src/com/expensetracker/service/*.java src/com/expensetracker/ui/*.java src/com/expensetracker/cli/*.java
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Build succeeded!
echo Starting Daily Expense Tracker GUI...
start javaw -cp bin com.expensetracker.Main
