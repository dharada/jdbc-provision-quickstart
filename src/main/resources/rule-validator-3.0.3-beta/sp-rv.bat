@ECHO OFF
REM
REM (c)Copyright 2023 SailPoint Technologies, Inc., All Rights Reserved.
REM
REM Run the Validator launcher
REM
set mypath=%~dp0
java -cp ./* -jar "%mypath%\sailpoint-saas-rule-validator.jar" %*