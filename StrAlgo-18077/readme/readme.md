# Tugas Besar STIMA #1: Tower Defense
Oleh:  
- Filbert Wijaya (13518077)  
- Difa Habiba Rahman (13518098)  
- Faris Rizki Ekananda (13518125)  

## Requirements
Install the Java SE Development Kit 8 for your environment here:  
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

Make sure JAVA_HOME system variable is set, Windows 10 tutorial here:   
https://www.mkyong.com/java/how-to-set-java_home-on-windows-10/

Install IntelliJ IDEA here: https://www.jetbrains.com/idea/download/  
The community edition is free.

## Cara Build Bot dan Menjalankannya
### Windows
1. Buka pom.xml pada di IntelliJ IDEA
2. Buka "Maven Projects" Tab di sebelah kanan
3. Pilih grup java-sample-bots > Lifecycle
4. Pilih Install
5. Untuk menjalankan bot, jalankan starter-pack/run.bat
   
### Linux
1. Pastikan Maven sudah terinstall dengan cara:
    ```
    sudo apt-get install mvn
    ```
2. Buka shell di folder bot
3. Jalankan perintah berikut:
    ```
    mvn install
    make run
    ```  
## Cara Menggunakan Bot Executable Jar 
1. Taruh jar bot pertama di **`starter-pack/starter-bots/java/target`** dengan nama  
   **`java-sample-bot-jar-with-dependencies.jar`**
2. Taruh jar bot kedua di **starter-pack/reference-bots/java/target** dengan nama  
   **`reference-bot-jar-with-dependencies.jar`**
3. Setel **`game-runner-config.json`**, pastikan setelan playernya adalah
   ```
   "player-a": "./starter-bots/java",
   "player-b": "./reference-bot/java",
   ```
   Apabila ingin menggunakan input sendiri, gunakan `"console"` sebagai playernya
4. Jalankan **`run.bat`** atau jalankan perintah berikut di shell direktori **`starter-pack/`**:
   ```
   java -jar tower-defence-runner-3.0.2.jar
   ```