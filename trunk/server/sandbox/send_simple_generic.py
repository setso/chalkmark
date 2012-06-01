#!/usr/bin/env python
import pika
import json
connection = pika.BlockingConnection(pika.ConnectionParameters(
        host='localhost'))
channel = connection.channel()



channels={};
#channel.queue_declare(queue='elasticsearch')
args={}
args['location']  = '33.8670,-118.19572'
args['location'] = '33.885104,-118.41095'
args['location'] = '35.265735,-116.07471'
#args['location'] = '33.884042,-118.41055'
args['radius'] = '100'
args['key']  = 'AIzaSyCkYaWM90ofPKMBQLWPZHM2Pnw6pMg6SXY'
args['key']  = 'AIzaSyDQFFnogfQkdMv_xk2CeHIpMLvPJNEylZw'
args['types'] = 'food';
args['sensor'] = 'false';
args_string = json.dumps(args)
base_url = "https://maps.googleapis.com/maps/api/place/search/json?";
arg_list = [(al+"="+args[al]+"&") for al in args]
url=base_url+"".join(arg_list)[:-1]
#url = "https://maps.googleapis.com/maps/api/place/search/json?key=AIzaSyCkYaWM90ofPKMBQLWPZHM2Pnw6pMg6SXY&radius=100&sensor=false&location=33.885104,-118.41095&types=food"

#args['db'] = 'datasources'
#args['collection'] = 'Farmers_Markets'
#args['location']  = '33.8670,-118.19572'
#args_string = json.dumps(args)


#channel.basic_publish(exchange='',
#                      routing_key='test_q',
#                      body='datga')

print url
channel.basic_publish(exchange='test_x',routing_key='test_q',body=url)
print " [x] Sent 'Hello World!'"
connection.close()
