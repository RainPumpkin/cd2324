Pressupostos:
-Só pode correr um Server por VM;
-Todas as VMs têm Java 11.

Instruções de execução:
-Instalar os 3 contractos;
-Criar os JARs do Server, Cliente, Register e MarkApp;
-Executar o Register "java - jar RegisterServer-1.0-jar-with-dependencies.jar [Port]" numa VM;
-Noutra(s) VM(s), instalar o docker, copiar o dockerfile e o jar do MarkApp para a(s) VM(s);
-Executar "sudo docker build -t g09/markapp .", criar a pasta "/home/CD2324-G09/images";
-Executar o Server "java -jar RegisterServer-1.0-jar-with-dependencies.jar {ServerIP} {RegisterIP}";
-Executar o Cliente "java -jar Client-1.0-jar-with-dependencies.jar {RegisterIP} {RegisterPort}".

