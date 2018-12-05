#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#     _   ____      ______           ______
#    / | / / /___  / ____/________ _/ __/ /_
#   /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
#  / /|  / / /_/ / /___/ /  / /_/ / __/ /_
# /_/ |_/_/ .___/\____/_/   \__,_/_/  \__/
#        /_/
#

+---------------------------------------+
| Data Probe - Command Line Application |
+---------------------------------------+

1. --< Overview >--
Data Probe is a secure application with the end-to-end encryption that you need to install to
provide access to your data sources. It is a Java-based application that is responsible for
deploying and hosting user defined models. It can be used as a command line utility in production
settings or as Maven-based Java library to start in-process for convenient model development and
testing. Note that one of the main tasks of Data Probe is to deploy, hot-redeploy and host user
prepared and provided models as JAR files.

2. --< Prerequisites >--
Data Probe is a Java application and you need to have Java SE Runtime Environment (JRE) ver. 8 or
later installed prior to running it. You can find download and installation instructions for
Java SE JRE at http://www.oracle.com/technetwork/java/javase/downloads/index.html

You can check if you have JRE installed and its version by running:
 `java -version`
 
3. --< Running >--
Data Probe is packaged into a single JAR file with all dependencies.
It can be launched in a standard way as any other executable JAR application:
 `java -jar probe-x.x.x-all-dependencies.jar`

4. --< Examples >--
 `java -jar probe-x.x.x-all-dependencies.jar -?`
 Prints help information about parameters.

 `java -jar probe-x.x.x-all-dependencies.jar`
 Starts with all default parameters.

 `java -jar probe-x.x.x-all-dependencies.jar -email=user@email.com -token=ASD12126GF`
 Starts with specified email and company token.