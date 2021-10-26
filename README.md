##  Intro
So, you’ve chosen Kafka as your messenger service, but you keep hearing about all of its headaches, like using Zookeeper and issues regarding rebalancing. Well, [Vectorized’s Redpanda](https://vectorized.io/redpanda) got you covered.
### What exactly [RedPanda](https://vectorized.io/redpanda) is and _why is it better than the others?_
Redpanda is a streaming platform, 100% Kafka API compatible for mission-critical workloads built for modern apps. Their pros include: 
-	Reliable message delivery 
-	10x faster speed than regular Kafka
-	Ultra-low latencies (due to their thread-per-core architecture) 
-	Reduced operational complexity 
-	Production ready
-   Is [free and source available](https://github.com/vectorizedio/redpanda)

##  First Steps
Now that I have your attention, let’s get started on using it!  
[TL;DR version](#tldr)   
It is recommended that you use a Linux-based machine to develop and operate it (I mean, who’s buying Windows’ license in a cloud environment anyway?), but if you’re a stubborn Windows user like myself, I got you covered.  
Alright, so for Linux users this part is extremely easy, and you just have to use this command below.  
```console
curl -1sLf 'https://packages.vectorized.io/nzc4ZYQK3WRGd9sy/redpanda/cfg/setup/bash.rpm.sh' | 
sudo -E bash && sudo yum install redpanda -y 
&& sudo systemctl start redpanda   
```
...and boom! It’s ready to use!   
If you’re a Windows user, let's do a nice workaround.     
This will require that you have installed at your machine [WSDL2](https://docs.microsoft.com/en-us/windows/wsl/install) and [Docker for Windows](https://docs.docker.com/desktop/windows/install/).    
Don’t forget that in order for Docker for Windows to work, you have to enable your Hipervisor service at the Control Panel, [more on that here.](https://docs.microsoft.com/en-us/virtualization/hyper-v-on-windows/quick-start/enable-hyper-v) 

With Docker installed and ready to use, copy and paste this and Save it as docker-compose.yml 
```console 
version: '3.7'
services:
  redpanda:
    command:
    - redpanda
    - start
    - --smp
    - '1'
    - --reserve-memory
    - 0M
    - --overprovisioned
    - --node-id
    - '0'
    - --kafka-addr
    - PLAINTEXT://0.0.0.0:29092,OUTSIDE://0.0.0.0:9092
    - --advertise-kafka-addr
    - PLAINTEXT://redpanda:29092,OUTSIDE://localhost:9092
    # NOTE: Please use the latest version here!
    image: docker.vectorized.io/vectorized/redpanda:v21.9.5
    container_name: redpanda-1
    ports:
    - 9092:9092
    - 29092:29092
```   
  
In the directory that you saved the file, open your CMD.    
One neat trick to open the CMD at the directory that you want is to open Windows Explorer at that directory.
![CMD trick](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/cmd-trick.png)

Click at the address bar. And change it to cmd.exe. Then it will open your CMD at your directory level.   
![CMD trick](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/cmd-trick2.png)
 
Pretty cool, right?   
Now, at the CMD type 
```console
docker-compose up –d
```
If everything is correct, you’ll see this beautiful screen   
![docker CMD ok](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/docker-compose.png)
 
And if you’re skeptical, like me, you might wanna check Docker for Desktop too.      
![docker desktop ok](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/docker-for-desktop.png)    
And now it’s all good. It’s already ready to use.   
## Start streaming
Now, Linux users have it easier again, because the official docs make it very very easy to do some testing. We don’t have anything official for Windows yet. So if you know any good replacement for rpk on Windows, let me know.   
At Linux,
1. Create a topic running this: 
   ```console
   docker exec -it redpanda-1 \
   rpk topic create my_topic --brokers=localhost:9092
   ```
2. Produce some messages to the topic running this:
```console
docker exec -it redpanda-1 \
rpk topic produce my_topic --brokers=localhost:9092
```

3. Type text into the topic and press Ctrl + D to seperate between messages.   
Press Ctrl + C to exit the produce command.

4. Consume (or read) the messages in the topic:
```console
docker exec -it redpanda-1 \
rpk topic consume my_topic --brokers=localhost:9092
```

Each message should be something like this   
```console
{
"message": "How do you stream with Redpanda?\n",
"partition": 0,
"offset": 1,
"timestamp": "2021-02-10T15:52:35.251+02:00"
}
```
## Using a client
Now that everything is good and running, let’s attach a client and actually see the magic happening. BTW, here’s a list of officially [supported clients.](https://vectorized.io/docs/faq/#What-clients-do-you-recommend-to-use-with-Redpanda)   
For this tutorial, I’ll use Java __(yeah, I know, I know…)__   
So, let’s jump into [Spring Initializr](https://start.spring.io/) and start exploring.    
On the dependencies page make sure you have at least Spring for Apache Kafka.
![spring kafka](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/spring%20initializr.png)  
 
For this tutorial, I’ll be using Spring Web as well. So, if you’re following my steps, you should have something like this:
![spring kafka](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/spring-depencies.png)   

 
Press Generate, and download your .zip file.   
Save it any place that you want (I used the same folder as our docker-compose.yml).    
Extract and open with your IDE. For Java, there’s nothing better than [IntelliJ](https://www.jetbrains.com/idea/), so I’ll stick with this one ([VSCode](https://code.visualstudio.com/) is also awesome! )   
Open your project with the pom.xml file   
![intellij pom](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/intellij-start.png)     
 
Select “Open as Project” and let’s start building some stuff.   
## First configs in our Java Client
Let’s start off by creating our **KafkaAdminConfig** class   
This class will help us to create our first topic (if there’s none).   
```java
@Configuration
public class KafkaAdminConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }
    //this bean will see if there's a topic already created, if not, it will create it for you
    @Bean
    public NewTopic initialTopic() {
        return new NewTopic(Constants.TOPIC, Constants.NUM_PARTITIONS,  Constants.REPLICATION_FACTOR);
    }
}
``` 
For better maintainability I created a **Constants** file too that contains this:  
```java 
public class Constants {

    public static final String TOPIC = "myTopic";
    public static final String GROUP_ID = "groupId";
    public static final Integer NUM_PARTITIONS = 1;
    public static final Short REPLICATION_FACTOR = 1;
}
```

Don’t forget to fill your application.properties file with this. You can even put your groupID here if you want.   
```java
spring.kafka.bootstrap-servers=localhost:9092
```

## Creating a producer
Our **KafkaProducer** class will look like this.   
```java
@Configuration
public class KafkaProducerConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapAddress);
        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```
**VERY IMPORTANT**. Check if the import of StringSerializer is from Kafka and not from Jackson. If you have it wrong, your application won't start **at all.**   
Make sure you have this as your import
```java
import org.apache.kafka.common.serialization.StringSerializer;
```   

Then we’ll create a **KafkaPublisher**, whose job is just to send the message to the topic   
```java
@Component
public class KafkaPublisher {


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topicName, String msg) {
        kafkaTemplate.send(topicName, msg);
    }
}
```
   
And for making things easier, I created a **RestController** to easily interact with our program   
```java
@RestController
public class KafkaController {

    @Autowired
    private KafkaPublisher kafkaPublisher;

    @PostMapping("publish")
    public ResponseEntity<String> publishMessage(@RequestParam String message){
        try {
            kafkaPublisher.sendMessage(Constants.TOPIC,message);
            return ResponseEntity.ok(null);

        } catch (Exception e ){
            return ResponseEntity.internalServerError().body(e.getLocalizedMessage());
        }
    }

}
```

If everything is correct, you should be able to start your application through the main Spring class.   
After starting your application, to check if everything is correct, send a message to yourself.   
```console
curl --location --request POST 'http://localhost:8080/publish?message=Hey,%20it%20works!'
```
But, sending messages with no one listening is no fun, so let’s create a consumer.   
## Creating a Consumer
Let’s do some small housekeeping again, by creating our **KafkaConsumerConfig** class   
```java
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapAddress);
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                Constants.GROUP_ID);
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

Again, check to see if you got the **correct imports**   
```java
import org.apache.kafka.common.serialization.StringDeserializer;
```

This one is the easiest of them all. Just create a component that will keep reading and printing your messages at your console.   
```java
@Component
public class KafkaConsumer {


    @KafkaListener(topics = Constants.TOPIC, groupId = Constants.GROUP_ID)
    public void listenGroupFoo(String message) {
        System.out.println("Received Message in group myGroup: " + message);
    }
}
```

Now, send that message again, and if everything goes well you’ll see something like this:
![working message](https://github.com/Deflaimun/redpanda-spring/blob/main/tutorial-images/working-message.png)
 
Well, that’s all I had. If you have any comments or suggestions, please let me know.    
Also if you wanna go more in-depth of this check Vectorize’s official website.   
https://vectorized.io/   
Join their slack community   
https://vectorizedcommunity.slack.com/ssb/redirect   
And check their code at GitHub   
https://github.com/vectorizedio/redpanda   
Explainer video on what RedPanda is and what it tries to accomplish   
https://www.youtube.com/watch?v=wwU58YMgPtE

## TL;DR
Clone [this repo](https://github.com/Deflaimun/redpanda-spring)
Hit   
```console 
docker-compose up –d
```
And start using your Java Client!
