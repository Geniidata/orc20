Orc20-indexer
================
This library fully implements the specification protocol of orc-20.

Developers can integrate this library in the code according to their needs.

### Files
```orc20_balance_snapshot_before_800010.txt```  _At block height #800010, we took a snapshot of the orc20 balance(excluding #800010) and saved it into this file. After block #800010, all new transactions will follow [OIP-10](https://docs.orc20.org/oips/oip-10). file md5: ff5a3b645b5033b18f641d30ce018046_

### Building

Orc20-Indexer uses Maven to build the project:

```shell
mvn clean package
```

### Running
args:
```shell
usage: Indexer
 -c,--content <arg>    Input file path for ORC20 inscription contents
 -t,--transfer <arg>   Input file path for ORC20 inscription transfers
```
command:
```shell
java -cp ./target/ordinals-orc20-indexer.jar \
 com.geniidata.ordinals.orc20.indexer.Indexer \
 -c ./target/classes/orc20_inscription_content_inputs.txt \
 -t ./target/classes/orc20_inscription_transfers_inputs.txt
```
output example:
```shell
################ balance summary ################
Address                                                         	Tick              	Inscription Number	Cash Balance      	Credit Balance
114s4QJEH2tyAcpAMFpBkMNWgNmEoDe5MT                              	ape               	3388855           	800000            	0
131hfn2Jtb6crsoE6mxWafp5nv3cq1LqPP                              	ordi              	3387885           	1000              	0
14j8P9xu7btH3As3bVUVrhsPToVx6Le7uo                              	ordinals          	3388532           	5000              	0
......
################ balance dump ################
{"tickId":"e23b794e69f1f2339504075f59061938880b723bb941eebe3e29775637993f0fi0","tick":"ape","inscriptionId":"9173e2c637a194b778f7f6f5cdf6d8555ef2505fa4ac5539449b4ffce8e1f233i0","balance":100000,"address":"114s4QJEH2tyAcpAMFpBkMNWgNmEoDe5MT","creator":null,"nonce":0,"balanceStatus":"OK","op":"MINT"}
{"tickId":"e23b794e69f1f2339504075f59061938880b723bb941eebe3e29775637993f0fi0","tick":"ape","inscriptionId":"bf5cffec2170d9fa88107c320cca0540496c2568efe28c14e8575a62b963783ci0","balance":100000,"address":"114s4QJEH2tyAcpAMFpBkMNWgNmEoDe5MT","creator":null,"nonce":0,"balanceStatus":"OK","op":"MINT"}
{"tickId":"e23b794e69f1f2339504075f59061938880b723bb941eebe3e29775637993f0fi0","tick":"ape","inscriptionId":"4477dd51171b3e94b60a2b86ec10d42f1ffd16d9bb222c9e21675e707a50945ei0","balance":100000,"address":"114s4QJEH2tyAcpAMFpBkMNWgNmEoDe5MT","creator":null,"nonce":0,"balanceStatus":"OK","op":"MINT"}
......
################ metadata dump ################
{"tickId":"49d3f5a2dc27d6d495dd3386a729113f296c3f8d939140adf2f2d3f7ec5764f3i0","tick":"ogmi","deployId":"1","inscriptionId":"49d3f5a2dc27d6d495dd3386a729113f296c3f8d939140adf2f2d3f7ec5764f3i0","inscriptionNumber":2498247,"deployer":"bc1ptu4w56sk644wm5q2yu3ra6d466fts2hvdd7c0aa0xtzarzlavcdskzsa9k","deployTime":1682840590,"max":115792089237316195423570985008687907853269984665640564039457584007913129639935,"minted":0,"limit":1,"decimals":18,"lastMintTime":0,"upgradeable":true,"content":"{\"p\": \"orc-20\",\"tick\": \"ogmi\",\"id\": \"1\",\"op\": \"deploy\",\"wp\": \"true\"}","upgradeTime":0,"wrapped":true}
{"tickId":"97b870b043d9745e674e2eb7bafbd9fa3dcdbbd776c671dab820710747ce9059i0","tick":"orc","deployId":"1","inscriptionId":"97b870b043d9745e674e2eb7bafbd9fa3dcdbbd776c671dab820710747ce9059i0","inscriptionNumber":2504160,"deployer":"bc1pfyv5cue8dw03vml464cjcn096aafmg40a7tsfes998dtufx7ghrswrcdw9","deployTime":1682841989,"max":21000000,"minted":21000000,"limit":10000,"decimals":18,"lastMintTime":1685502874,"upgradeable":false,"content":"{ \n  \"p\": \"orc-20\",\n  \"tick\": \"ORC\",\n  \"id\": \"2504160\",\n  \"op\": \"upgrade\",\n  \"v\": \"2\",\n  \"ug\": \"false\"\n}","upgradeTime":1686155613,"wrapped":false}
{"tickId":"2d5468cedb5d243552eef570f770996419a05397e5985d670ecd50334d8ddd30i0","tick":"pepe","deployId":"1","inscriptionId":"2d5468cedb5d243552eef570f770996419a05397e5985d670ecd50334d8ddd30i0","inscriptionNumber":3387929,"deployer":"bc1ppsfg0mu5rnu9naelp6cnuy72rz8u9dey38fm3mvpyqlqn9dmjufs46audv","deployTime":1683137053,"max":21000000,"minted":21000000,"limit":10000,"decimals":18,"lastMintTime":1683948590,"upgradeable":true,"content":"{ \n\"p\": \"orc-20\",\n\"tick\": \"pepe\",\n\"id\": \"1\",\n\"op\": \"deploy\",\n\"max\": \"21000000\",\n\"lim\": \"10000\"\n}","upgradeTime":0,"wrapped":false}
......
################ event dump ################
{"eventId":"00377d33bebbac6ac1f9747a23c133bcb55b30c8b1a446a0c195bab9fada5ecc:0:0","tickId":"00377d33bebbac6ac1f9747a23c133bcb55b30c8b1a446a0c195bab9fada5ecci0","tick":"ordi","inscriptionId":"00377d33bebbac6ac1f9747a23c133bcb55b30c8b1a446a0c195bab9fada5ecci0","inscriptionNumber":6251371,"fromAddress":"bc1pj5g77htdhd0lwm7a80x9fjsjedqwp2882yrldm4xdx9mut4qa8lqansjye","toAddress":"bc1pjrc604fua2lpvvlkd4z8546whyravx3hc0tyh4adhx5a59wlnkws6ufl0g","eventType":"INSCRIBE_DEPLOY","op":"DEPLOY","nonce":0,"creator":null,"eventStatus":"SUCCESS","eventErrCode":null,"amount":null,"extData":"{\"p\": \"orc-20\",\"tick\": \"ordi\",\"id\": \"6\",\"op\": \"deploy\",\"max\": \"21000000\", \"lim\": \"1000\"}","txId":"00377d33bebbac6ac1f9747a23c133bcb55b30c8b1a446a0c195bab9fada5ecc","txIndex":3414,"blockTime":1683992074,"blockHeight":789552}
{"eventId":"00390bf9d50278979a46446235b9dd6af5f16e7a3408f9eec7185162f6d7f7d9:0:0","tickId":"00390bf9d50278979a46446235b9dd6af5f16e7a3408f9eec7185162f6d7f7d9i0","tick":"exotics","inscriptionId":"00390bf9d50278979a46446235b9dd6af5f16e7a3408f9eec7185162f6d7f7d9i0","inscriptionNumber":6382548,"fromAddress":"bc1purr5msh78pjz0tzrj2xcrvptk5k25mrr28ew8mvpw54eq3yuldpqxut5eh","toAddress":"bc1pwuj3ftgljq8u6ealuwvuwzaetgtkytgenw8eg3032chz32p7adtsy804y7","eventType":"INSCRIBE_DEPLOY","op":"DEPLOY","nonce":0,"creator":null,"eventStatus":"SUCCESS","eventErrCode":null,"amount":null,"extData":"{ \n  \"p\": \"orc-20\",\n  \"tick\": \"exotics\",\n  \"id\": \"1\",\n  \"op\": \"deploy\",\n  \"max\": \"21000000\",\n  \"lim\": \"600\"\n}\n","txId":"00390bf9d50278979a46446235b9dd6af5f16e7a3408f9eec7185162f6d7f7d9","txIndex":771,"blockTime":1684022460,"blockHeight":789605}
{"eventId":"003e3ea4dbbd555c20b5c8ec2f02559dd18b5d2f0f8750b434ec35549346e885:0:0","tickId":"003e3ea4dbbd555c20b5c8ec2f02559dd18b5d2f0f8750b434ec35549346e885i0","tick":"ordinals","inscriptionId":"003e3ea4dbbd555c20b5c8ec2f02559dd18b5d2f0f8750b434ec35549346e885i0","inscriptionNumber":8120632,"fromAddress":"bc1plnd4ntmky3wqx9cy6fllv4ynvqmhssrcnvwh20hcm59mexkl990scxv539","toAddress":"bc1qs0rwvdtnnjy702ka5yjd5rasyjxucdnua72j4t","eventType":"INSCRIBE_DEPLOY","op":"DEPLOY","nonce":0,"creator":null,"eventStatus":"SUCCESS","eventErrCode":null,"amount":null,"extData":"{ \n  \"p\": \"orc-20\",\n  \"tick\": \"ordinals\",\n  \"id\": \"51\",\n  \"op\": \"deploy\",\n  \"max\": \"21000000\",\n  \"lim\": \"1000\"\n}","txId":"003e3ea4dbbd555c20b5c8ec2f02559dd18b5d2f0f8750b434ec35549346e885","txIndex":1025,"blockTime":1684600665,"blockHeight":790606}
......
################ balance oip10 snapshot dump ################
{"tickId":"e23b794e69f1f2339504075f59061938880b723bb941eebe3e29775637993f0fi0","tick":"ape","inscriptionId":"9173e2c637a194b778f7f6f5cdf6d8555ef2505fa4ac5539449b4ffce8e1f233i0","balance":100000,"address":"114s4QJEH2tyAcpAMFpBkMNWgNmEoDe5MT","creator":null,"nonce":0,"balanceStatus":"OK","op":"MINT"}
{"tickId":"e23b794e69f1f2339504075f59061938880b723bb941eebe3e29775637993f0fi0","tick":"ape","inscriptionId":"bf5cffec2170d9fa88107c320cca0540496c2568efe28c14e8575a62b963783ci0","balance":100000,"address":"114s4QJEH2tyAcpAMFpBkMNWgNmEoDe5MT","creator":null,"nonce":0,"balanceStatus":"OK","op":"MINT"}
{"tickId":"e23b794e69f1f2339504075f59061938880b723bb941eebe3e29775637993f0fi0","tick":"ape","inscriptionId":"4477dd51171b3e94b60a2b86ec10d42f1ffd16d9bb222c9e21675e707a50945ei0","balance":100000,"address":"114s4QJEH2tyAcpAMFpBkMNWgNmEoDe5MT","creator":null,"nonce":0,"balanceStatus":"OK","op":"MINT"}
......
```
### Requirements

- Java 1.8+
