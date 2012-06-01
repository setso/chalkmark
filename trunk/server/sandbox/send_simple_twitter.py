#!/usr/bin/env python
import pika
import json
connection = pika.BlockingConnection(pika.ConnectionParameters(
        host='localhost'))
channel = connection.channel()



channels={};
#channel.queue_declare(queue='elasticsearch')
args={}
location  = '33.884042,-118.410551'
radius = '1km'
args['q'] = '' 
args['geocode']  = location+","+radius;   
#args['rpp'] = '100'
#args['radius']= '10m' 
#base_url = http://search.twitter.com/search.json?q=&geocode=53.2739084,-7.4945478,50mi&rpp=100
base_url = "http://search.twitter.com/search.json?"
arg_list = [(al+"="+args[al]+"&") for al in args]
url=base_url+"".join(arg_list)[:-1]
print url
cool = "http://search.twitter.com/search.json?q=&geocode=33.8670,-118.19572,50mi&rpp=100"
print cool 
#url = "https://maps.googleapis.com/maps/api/place/search/json?key=AIzaSyCkYaWM90ofPKMBQLWPZHM2Pnw6pMg6SXY&radius=100&sensor=false&location=33.885104,-118.41095&types=food"
# http://search.twitter.com/search.json?q=&geocode=53.2739084,-7.4945478,50mi&rpp=100
#args['db'] = 'datasources'
#args['collection'] = 'Farmers_Markets'
#args['location']  = '33.8670,-118.19572'
#args_string = json.dumps(args)


#channel.basic_publish(exchange='',
#                      routing_key='test_q',
#                      body='datga')

#channel.basic_publish(exchange='test_x',routing_key='test_q',body=url)
print " [x] Sent 'Hello World!'"
connection.close()

def foo(location,radius,base_url):
		base_url = "http://search.twitter.com/search.json?"
		arg_list = [(al+"="+args[al]+"&") for al in args]
		url=base_url+"".join(arg_list)[:-1]
		print 	
