from pyparsing import *
import re
import pymongo
import sys

# hello
# another change from nehal
# change from setso

### You need
def f_l(line):
	""" Removes the first and last character of line """
	return line[1:-1]
def l(line):
	""" Removes the last character in the line"""
	return line[:-1]


def get_datachannel(channel_id):
	""" Retrieve channel information based on the channel_id"""
        try:
                mongo_conn = pymongo.Connection('localhost',27017)
                db = mongo_conn['data_channels']
                coll = db['datasrc']
		datachannel_info=coll.find({"datasrc_id":datasrc_id})
		return datachannel_info
      	except:
                print 'failed to save data channel information'
                sys.exit(-1);
	


def save_tomongo(data_dict):
	""" Save the datachannel information to MongoDB.  Remember you'll need
	     to have both MongoDB and pymongo driver installed"""
	try:
		datasrc_id=data_dict['datasrc_id']
	except:
		print 'No data src id found'
		sys.exit(-1); 

	try:
		mongo_conn = pymongo.Connection('localhost',27017)
		db = mongo_conn['data_channels']
		coll = db['datasrc']
		is_datasrcid=coll.find({"datasrc_id":datasrc_id})		
		if is_datasrcid.count() == 0:
			coll.save(data_dict)
		else:
			print 'Already in database'	
	except:
		print 'failed to save data channel information'
		sys.exit(-1); 

	return 0; 


def gen_datachannel(d_file):
	""" Read in data channel information for each channel and create
	    entry in the database.  Uses pyparsing to parse the information in 
	    the channel information file (d_file) 
    	"""	
	regex_args = re.compile("args"); 
	datasrc_end = re.compile("datasrc_end"); 
	datasrc_start = re.compile("datasrc_start"); 
	argflag=0; 
	arglist={} 
	channel_info={}
##
## define elements of the grammar
##
	backslash="/"
	colon =":"
	period="."
	sep = '&'
	quest='?'
	equal = Literal("=")
	lb = Literal("[")
	rb = Literal("]")
	W=Word(alphanums+"'"+"_"+","+"\""+sep+colon+backslash+period+quest)
	
	## the parse grammar
	g=W+equal+ZeroOrMore(lb)+Group(ZeroOrMore(W))+ZeroOrMore(rb)
	
	## Loop through the data file and try to parse it (line by line)
	## if a line fails on the parse try to see if it matches 
	##  
	##  datasrc_start
	##  datasrc_id=
	##  args_start
	##  args_end #		
	##  datasrc_end
	for line in open(d_file):
		try:
			parse_string=g.parseString(line).asList()
			## filter out the extract '[' or ']'
			parse_string = [i for i in parse_string if i != '[' and i !=']']
			if argflag == 0: 		
				channel_info[parse_string[0]] = parse_string[2]
			if argflag == 1:
				arglist[parse_string[0]] = parse_string[2]
		except:
			try:	
				## check to see if this is the 
				## the start/end of the argument list	
				if regex_args.search(line) != None:
					if re.search("start",line):
						argflag=1;
					if re.search("end",line):
						argflag = 0; 
			
				if datasrc_start.search(line) != None:
					print 'start of datasrc_id entry'
					 
				if datasrc_end.search(line)  != None:
					print 'end of datasrc_id entry' 
					channel_info['args'] = arglist
					print channel_info	
					save_tomongo(channel_info)	
					channel_info={}
			except:
				print 'exception'

def test():
	gen_datachannel("d.dat") 

#ord =channel_info["ord"][0].split(",")
#jj=""
#for order in ord:
#	jj=jj+f_l(order)+"="+arglist[f_l(order)][0]+"&"
#args=''.join([i for i in jj if i!="\'" and i!="\""])
#print l(f_l(channel_info['base_url'][0])+args)
