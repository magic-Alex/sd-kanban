$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'D:\root\dev\Java\jdk\jdk17' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$mavenRepo = if ($env:MAVEN_REPO_LOCAL) { $env:MAVEN_REPO_LOCAL } else { 'D:\root\dev\Java\maven\repository' }
mvn "-Dmaven.repo.local=$mavenRepo" clean package
