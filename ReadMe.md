**Fault Tolerant and Distributed Restful Api Servers**

A pair of [Akka](http://akka.io/) servers (backend & frontend servers) that leverages bitcoin core/daemon jsonrpc api.

The servers don't have to be in the same machine. They can be anywhere as long as they can communicate via tcp. The tcp settings can be adjusted in the ```src/main/resources/``` configuration files.

In the ```bin``` directory you can find 2 jars that we built for demo purposes with the default configuration settings which enforces that the servers and the bitcoin jsonrpc api are running in the same machine.

To run the servers first ensure that you have bitcoin core/daemon runing. Try this in a terminal window:
```
$ curl --user user:password --data-binary '{"jsonrpc": "1.0", "id":"curltest", "method": "getinfo", "params": [] }' -H 'content-type: text/plain;' http://127.0.0.1:8332/
```
Please note that you need to replace the user, password, interface and port in the cURL string above with the values you configured your bitcoin core/daemon. If all is good you should receive an appropriate json response.

For the reminder of this readme we are going to use [Httpie](https://pypi.python.org/pypi/httpie) for illustrating the interactions with the servers. *Please ensure that you have [Httpie](https://pypi.python.org/pypi/httpie) install in your system if you are going to run the following test or, ensure that you are using the correct cURL equivalent.*

Clone the repo and navigate in a terminal to the project root directory. To start the backend server do:
```
$ java -jar bin/btcfest-backend.jar
```
In a new terminal window start the frontend server:
```
$ java -jar bin/btcfest-frontend.jar
```
The order in which you started the servers don't matter! Now open another terminal to test our servers with [Httpie](https://pypi.python.org/pypi/httpie). Try this:

```
$ http GET localhost:5000/getinfo
```
The ```getinfo``` command of bitcoin jsonrpc api don't require parameters so the server accepts get requests or post requests with empty params array.
```
$ echo '{"params":"[]"}' | http POST localhost:5000/getinfo
```
The server's response should look like this:
```
HTTP/1.1 200 OK
Content-Length: 418
Content-Type: application/json; charset=UTF-8
Date: Mon, 24 Nov 2014 23:38:50 GMT
Server: Btcfest.com REST API

{
    "id": "btcfest-595892", 
    "result": {
        "balance": 0.0, 
        "blocks": 331486, 
        "connections": 10, 
        "difficulty": 40300030327.8914, 
        "errors": "", 
        "keypoololdest": 1415428822, 
        "keypoolsize": 101, 
        "paytxfee": 0.0, 
        "protocolversion": 70002, 
        "proxy": "", 
        "relayfee": 1e-05, 
        "testnet": false, 
        "timeoffset": -4, 
        "version": 90300, 
        "walletversion": 60000
    }
}
```
You can also pass an id parameter
```
$ echo '{"params":"[\"myaccount\"]", "id":"myspecialid"}' | http POST localhost:5000/getaddressesbyaccount
```
The response:
```
HTTP/1.1 200 OK
Content-Length: 191
Content-Type: application/json; charset=UTF-8
Date: Mon, 24 Nov 2014 23:44:08 GMT
Server: Btcfest.com REST API

{
    "id": "myspecialid", 
    "result": [
        "1PeHoazswTezkmPFUEnSJyQcCcQbFUkh6J", 
        "186ibbe2LkPJ8KNyNqEnTizXHJm4TpBbXh", 
        "1JoM4ou5EsAQg5acchBp4Gan4cHgZ7owUd", 
        "1F4i5UuvBGE7XVxG16hdv44yF9n1ByfvXC"
    ]
}
```
