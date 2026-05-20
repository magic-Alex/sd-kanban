$env:JAVA_HOME = 'D:\root\dev\Java\jdk\jdk17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" clean package
