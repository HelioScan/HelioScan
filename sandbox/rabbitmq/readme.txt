Uses the .NET client 2.7.0 from: http://www.rabbitmq.com/dotnet.html

1.) Download and install RabbitMQ: http://www.rabbitmq.com/download.html
2.) Start the RabbitMQ server: rabbitmq-server.bat
3.) Start receive.vi and send.vi
4.) Send messages from send.vi to receive.vi
5.) Stopping send.vi also stops receive.vi (by sending a "STOP" message).