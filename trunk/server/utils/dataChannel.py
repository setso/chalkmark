import pika
import sys
import json
import pymongo
from pika.adapters.tornado_connection import TornadoConnection

# Detect if we're running in a git repo
from os.path import exists, normpath
if exists(normpath('../pika')):
    sys.path.insert(0, '..')

from pika.adapters import SelectConnection
from pika.connection import ConnectionParameters

# We use these to hold our connection & channel


class dataChannel:
	""" The dataChannel is the base class of all our datasource.  
	It's purpose is to: a).  Setup the queues"""
	def __init__(self,ds_name):
		self.channel = None
		self.dc_id = "eek"#dc_id 
		## query mongoDB to find all the particulars about this
		## data channel including:  which queue is listening to, 
		## which exchange, the routing key..etc. 
		self.ret_queue  = "ret_queue"
		self.connection  = None
		self.channel = None
		self.connected = False; 
		self.connecting = False; 	
		self.exchange = "test_x";
		self.queue = "test_q"  	
		self.routing_key = "test_q"  	
		## use the ds to the find which exchange and which queue this 
		## datachannel listens	

	def mongo_db(self):
		## connect to the mongodb
		mongo_conn = Connection('localhost',27017)
		db = mongo_conn['data_channels']
		coll= db['bbox_pts']


	def connect(self):
		print self
        	if self.connecting:
            		print ('1...PikaClient: Already connecting to RabbitMQ')
            		return
        	print ('1...PikaClient: Connecting to RabbitMQ on localhost:5672')
        	self.connecting = True
        	credentials = pika.PlainCredentials('guest', 'guest')
        	param = pika.ConnectionParameters(host='localhost',
                                          	port=5672,
                                          	virtual_host="/",
                                          	credentials=credentials)
       
 		
		host = (len(sys.argv) > 1) and sys.argv[1] or '127.0.0.1'
		self.connection = SelectConnection(ConnectionParameters(host),
                                            	self.on_connected)
		if self.connection != None:
			print self.connection	
			print 'connection'
		
	
	def on_connected(self,connection):
	        print '2...PikaClient: Connected to RabbitMQ on localhost:5672'
		self.connection = connection
		self.connection.channel(self.on_channel_open)
		self.connected = True 


	def on_channel_open(self, channel):
        	print ('3...PikaClient: Channel Open, Declaring Exchange')
        	self.channel = channel
        	self.channel.exchange_declare(exchange=self.exchange,
                                      	type="direct",
                                      	auto_delete=False,
                                      	durable=True,
                                      	callback=self.on_exchange_declared)


	def on_exchange_declared(self, frame):
        	print ('4...PikaClient: Exchange Declared, Declaring Queue')
        	self.channel.queue_declare(queue=self.queue,
                                   	auto_delete=False,
                                   	durable=True,
                                   	exclusive=False,
                                   	callback=self.on_queue_declared)

    	def on_queue_declared(self, frame):
       		print('5...PikaClient: Queue Declared, Binding Queue')
		print "demo_receive: Queue Declared"
#		self.channel.basic_consume(self.handle_delivery, queue='test_q')
		self.channel.queue_bind(exchange=self.exchange,
                			queue=self.queue,
                                	routing_key=self.routing_key,
                                	callback=self.on_queue_bound)
		
	def on_queue_bound(self, frame):
        	print('6...PikaClient: Queue Bound, Issuing Basic Consume')
        	self.channel.basic_consume(consumer_callback=self.handle_delivery,
                                   	queue=self.queue)

		
	def handle_delivery(self,channel, method_frame, header_frame, body):
    		print "7...Basic.Deliver %s delivery-tag %i: %s" %\
          		(header_frame.content_type,
          		method_frame.delivery_tag,
           		body)
		print body
		channel.basic_ack(delivery_tag=method_frame.delivery_tag)


	def get_data(self,args):
			print "Please implement get_data"
		


#dr = dataChannel("test");
#dr.connect();
#dr.connection.ioloop.start();
