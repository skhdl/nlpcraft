<pre>
    _   ____      ______           ______ 
   / | / / /___  / ____/________ _/ __/ /_
  /  |/ / / __ \/ /   / ___/ __ `/ /_/ __/
 / /|  / / /_/ / /___/ /  / /_/ / __/ /_  
/_/ |_/_/ .___/\____/_/   \__,_/_/  \__/  
       /_/   
</pre>

### Overview
`NLPCraft` library allows you build advanced _free-form natural language interface_ to any 
public or private data sources like databases, REST services, IoT devices, 
voice assistants, chatbots, etc.

`NLPCraft` is the core module in [DataLingvo](https://www.datalingvo.com) enterprise-grade cloud-based service and has been in 
development since 2013. It has been refactored and enhanced as a highly functional standalone 
NLP system and opened to the community in 2019. 

### Introduction
How does it work?

To start using NLPCraft you need to define a model using Data Model API. A model specifies how to interpret user input, how to query or control a particular data source, and finally how to format the result back to the user. Currently, Data Model API is Java-based but more language support is on its way.

Once you defined your model you will need to deploy it into a data probe - an application that you need to run. As you’ll learn later a data probe is a secure application that employs end-to-end encryption and router ingress-only connectivity. Its purpose is to deploy and manage user data models. Each data probe can host multiple models, and you can have multiple data probes. Data probes can be deployed and run anywhere as long as there is an outbound connectivity, and are typically deployed in DMZ or close to your private data sources. Note that DataLingvo can be deployed on-premise in which case no outbound connectivity is required at all.

Once your model is defined and deployed into a data probe, your users can start using standard web interface or REST-integrated custom applications. Either way users can now ask natural language questions and get results back - while developers can perform human linguistic curation, data model development and training among many other administrative and data science tasks.
### License

[Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) with [Commons Clause](https://commonsclause.com/).

### Prerequisites
Here's what you will need to get started with `nlpcraft`:
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


