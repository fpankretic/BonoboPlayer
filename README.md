# BonoboPlayer

To start the project, minimal configuration is needed. Create `.env` file in the root directory and add the following content:

```env
DISCORD_API_TOKEN="insert your discord bot token here"
IPV6_ENABLED="false"
PREFIX="insert your prefix here"                        # Example: !
PO_TOKEN="insert your po token here"
VISITOR_DATA="insert your visitor data here"
REFRESH_TOKEN=""                                        # Leave empty for automatic generation
```

To get `PO_TOKEN` and `VISITOR_DATA` you can check out this [link](https://github.com/iv-org/youtube-trusted-session-generator).

Now you can run the project with the following commands:

```bash
mvn clean package
mvn -q exec:java -Dexec.mainClass="MainKt"
```
s
