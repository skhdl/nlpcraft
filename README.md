<img src="http://nlpcraft.org/images/nlpcraft_logo_black.gif" height="80px">

### Overview
NLPCraft library allows you build advanced **free-form natural language interface** to any 
public or private data sources like databases, REST services, IoT devices, 
voice assistants, chatbots, etc.

NLPCraft is the core module in [DataLingvo](https://www.datalingvo.com) enterprise-grade 
cloud-based service and has been in development since 2013. It has been enhanced as a highly 
functional standalone NLP system and opened to the community in 2019. 

### Introduction
How does it work?

You start by defining a model with a simple Data Model API using any JVM-based 
language like Java, Scala, Groovy, etc. For a particular data source, a model-as-a-code specifies 
how to interpret user input 
(e.g. intents), how to query or control this data source, and how to format the 
final result back. 

Once your model is defined you will need to deploy it into a NLPCraft Data Probe - an application that 
connects to NLPCraft Server. Its purpose is to host user data models. You can have multiple data
probes and each data probe can host multiple data models. Data probes are typically deployed in 
DMZ or close to your private data sources. 

Once your model is defined and deployed, you can start NLPCraft Server and use its 
REST API to ask natural language questions and get results back from your configured data source.
 
### License

[Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) with [Commons Clause](https://commonsclause.com/).

### Prerequisites
Here's what you will need to get started with NLPCraft:
 - [Java SE Runtime Environment](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (JRE) ver. 8 or later installed.
 - Latest [Git](https://git-scm.com/downloads) and [Maven](https://maven.apache.org/install.html).
 
### Clone This Project
Clone this project to a local folder:
```shell
$ mkdir nlpcraft
$ cd nlpcraft
$ git clone https://github.com/vic64/nlpcraft.git
```

### Documentation

### Copyright
Copyright (C) 2013-2019 [DataLingvo Inc.](https://www.datalingvo.com) All Rights Reserved.


