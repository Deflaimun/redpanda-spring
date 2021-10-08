##Intro
So, you’ve chosen Kafka as your messenger service, but you keep hearing about all of its headaches, like using Zookeeper and issues regarding rebalancing. Well, Vectorized’s Redpanda got you covered.
####What exactly RedPanda is and why is it better than the others?
Redpanda is a streaming platform, 100% Kafka API compatible for mission-critical workloads built for modern apps. Their pros include: 
-	Reliable message delivery 
-	10x faster speed than regular Kafka
-	Ultra-low latencies (due to their thread-per-core architecture) 
-	Reduced operational complexity 
-	Production ready

##First Steps
Now that I have your attention, let’s get started on using it! 
TL;DR version
It is recommended that you use a Linux-based machine to develop and operate it (I mean, who’s buying Windows’ license in a cloud environment anyway?), but if you’re a stubborn Windows user like myself, I got you covered. 
Alright, so for Linux users this part is extremely easy, and you just have to use this command below.
curl -1sLf 'https://packages.vectorized.io/nzc4ZYQK3WRGd9sy/redpanda/cfg/setup/bash.rpm.sh' | sudo -E bash && sudo yum install redpanda -y && sudo systemctl start redpanda
...and boom! It’s ready to use!
If you’re a Windows user, you have a nice workaround. This will require that you have installed at your machine WSDL2 (https://docs.microsoft.com/en-us/windows/wsl/install) and Docker for Windows (https://docs.docker.com/desktop/windows/install/). Don’t forget that in order for Docker for Windows to work out, you have to enable your Hipervisor service at the Control Panel. More on that here (https://docs.microsoft.com/en-us/virtualization/hyper-v-on-windows/quick-start/enable-hyper-v) 

With Docker installed and ready to use, copy and paste this docker-compose file 
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
    image: docker.vectorized.io/vectorized/redpanda:v21.7.6
    container_name: redpanda-1
    ports:
    - 9092:9092
    - 29092:29092

Save it as docker-compose.yml
In the directory that you saved the file, open your CMD. 
(One neat trick to open the CMD at the directory that you want is to open Windows Explorer at that directory.)
 
Click at the address bar. And change it to cmd.exe. Then it will open your CMD at your directory level. 
 
Pretty cool, right?
Hit 
docker-compose up –d
If everything is correct, you’ll see this beautiful screen
 
And if you’re skeptical, like me, you might wanna check Docker for Desktop too.
 
And now it’s all good. It’s already ready to use.
##Start streaming
Now, Linux users have it easier again, because the official docs make it very very easy to do some testing. We don’t have anything official for Windows yet. So if you know any good replacement for rpk on Windows, let me know.
At Linux,
1-  Create a topic running this: 
docker exec -it redpanda-1 \
rpk topic create my_topic --brokers=localhost:9092
2 – Produce some messages to the topic running this:
docker exec -it redpanda-1 \
rpk topic produce my_topic --brokers=localhost:9092

Type text into the topic and press Ctrl + D to seperate between messages.

Press Ctrl + C to exit the produce command.
3 - Consume (or read) the messages in the topic:
docker exec -it redpanda-1 \
rpk topic consume twitch_chat --brokers=localhost:9092
Each message should be something like this
{
"message": "How do you stream with Redpanda?\n",
"partition": 0,
"offset": 1,
"timestamp": "2021-02-10T15:52:35.251+02:00"
}
##Using a client
Now that everything is good and running, let’s attach a client and actually see the magic happening. BTW, here’s a list of officially supported clients (https://vectorized.io/docs/faq/#What-clients-do-you-recommend-to-use-with-Redpanda)
For this tutorial, I’ll use Java (yeah, I know, I know….) 
So, let’s jump into Spring Initializr (https://start.spring.io/) and start exploring. On the dependencies page make sure you have at least Spring for Apache Kafka.
 
For this tutorial, I’ll be using Spring Web as well. So if you’re following my steps, you should have something like this
 
Press Generate, and download your .zip file.
Save it any place that you want (I used the same folder as our docker-compose.yml). Extract and open with your IDE. For Java, there’s nothing better than IntelliJ, so I’ll stick with this one (VSCode is also awesome! )
Open your project with the pom.xml file
 
Select “Open as Project” and let’s start building some stuff.
##First configs in our Java Client
Let’s start off by creating our KafkaAdminConfig class
This class will help us to create our first topic (if there’s none).
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
    //this bean will see if there's a topic already created, if not, it will create it
    @Bean
    public NewTopic initialTopic() {
        return new NewTopic(Constants.TOPIC, Constants.NUM_PARTITIONS,  Constants.REPLICATION_FACTOR);
    }

For better maintainability I created a Constants file too that contains this
public class Constants {

    public static final String TOPIC = "myTopic";
    public static final String GROUP_ID = "groupId";
    public static final Integer NUM_PARTITIONS = 1;
    public static final Short REPLICATION_FACTOR = 1;
}

Don’t forget to fill your application.properties file with this. You can even put your groupID here if you want.
spring.kafka.bootstrap-servers=localhost:9092

##Creating a producer
Our producer class will look like this.
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
VERY IMPORTANT. Check if the import of StringSerializer is from Kafka and not from Jackson. Make sure you have this as your import
import org.apache.kafka.common.serialization.StringSerializer;

Then we’ll create a KafkaPublisher, whose job is just to send the message to the topic
@Component
public class KafkaPublisher {


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topicName, String msg) {
        kafkaTemplate.send(topicName, msg);
    }
}

And for making things easier, create a RestController to easily interact with our program
@RestController
public class KafkaController {

    @Autowired
    private KafkaPublisher kafkaPublisher;

    @PostMapping("publish")
    public ResponseEntity<String> publishMessage(@RequestParam String message){
        try {
            kafkaPublisher.sendMessage(Constants.TOPIC,message);
            return ResponseEntity.ok(null);

        }catch (Exception e ){
            return ResponseEntity.internalServerError().body(e.getLocalizedMessage());
        }
    }

}

If everything is correct, you should be able to start your application through the main Spring class.
To check if everything is correct, send a message to yourself.
curl --location --request POST 'http://localhost:8080/publish?message=Hey,%20it%20works!'
But, sending messages without no one listening is no fun, so let’s create a consumer.
##Creating a Consumer
Let’s do some small housekeeping again, by creating our KafkaConsumerConfig class
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

Again, check to see if you got the correct imports
import org.apache.kafka.common.serialization.StringDeserializer;

This one is the easiest of them all. Just create a component that will keep reading and printing your messages at your console.
@Component
public class KafkaConsumer {


    @KafkaListener(topics = Constants.TOPIC, groupId = Constants.GROUP_ID)
    public void listenGroupFoo(String message) {
        System.out.println("Received Message in group myGroup: " + message);
    }
}

Now, send that message again, and if everything goes well you’ll see something like this
 
Well, that’s all I had for today. If you wanna go more in-depth of this check their Vectorize’s official website. If you have any comments or suggestions, please let me know. 
https://vectorized.io/
Join their slack community
https://vectorizedcommunity.slack.com/ssb/redirect
And check their code at GitHub
https://github.com/vectorizedio/redpanda

##TL;DR
Clone this repo (https://github.com/Deflaimun/redpanda-spring)
Hit docker-compose up –d
And start using your Java Client!