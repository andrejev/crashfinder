# crashfinder
An approach for localization of regression errors based on the stack trace of a failing test and differences between two versions.
A Jenkins plugin based on this framework exists ([crashfinder-plugin](https://github.com/heiqs/crashfinder-plugin)).

# CL interface

## Usage
mvn exec:java -Dexec.mainClass="de.hdu.pvs.crashfinder.Main" -Dcrashfinder.config="/home/andrejev/ws/restapi-jira/config-YARN-5725-YARN-5430.txt"

## Config file parameters

```
crashfinder.description.issue=HDFS-3856
crashfinder.description.issue.version=b29cb2d99756c3ae56ab12fac38d95668b8eb2f1
crashfinder.description.is_broken_by=HADOOP-8689

crashfinder.description.is_broken_by.version=18c5bc86ca256beb9d4ccd6588c0b0ebe9dfcbd0

# cd /home/andrejev/ws/passing; git checkout 31cb5a7fa554f38471f9962cbdc25aabb002b6fd; mvn clean package -DskipTests; mvn test -DskipMain -Dtest=TestHDFSServerPorts
crashfinder.passing.path=/home/andrejev/ws/hadoop-verify-hdfs-3865/passing
crashfinder.passing.jar=hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-3.0.0-SNAPSHOT.jar
crashfinder.passing.version=31cb5a7fa554f38471f9962cbdc25aabb002b6fd
crashfinder.passing.classpath=/home/andrejev/ws/hadoop-verify-hdfs-3865/passing

# cd /home/andrejev/ws/failing; git checkout 24e47ebc18a62f6de351ec1ab8b9816999cc3267; mvn clean package -DskipTests; mvn test -DskipMain -Dtest=TestHDFSServerPorts
crashfinder.failing.path=/home/andrejev/ws/hadoop-verify-hdfs-3865/failing
crashfinder.failing.jar=hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-3.0.0-SNAPSHOT.jar
crashfinder.failing.version=24e47ebc18a62f6de351ec1ab8b9816999cc3267
crashfinder.failing.classpath=/home/andrejev/ws/hadoop-verify-hdfs-3865/failing


crashfinder.testcase.name=TestHDFSServerPorts
crashfinder.testcase.stacktrace=hadoop-hdfs-project/hadoop-hdfs/target/surefire-reports/org.apache.hadoop.hdfs.TestHDFSServerPorts-output.txt
#crashfinder.testcase.seed=%SEED%

crashfinder.exclusion.path=src/resources/JavaAllExclusions.txt
```
