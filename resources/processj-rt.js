__RT = function() {
    //////
    // Scheduler logic
    //////
    
    var runQueue = [];
    
    var schedule = function(p) {
	runQueue.push(p);
    }
    
    var main = function() {
	while (runQueue.length !== 0) {
	    p = runQueue.pop();
	    if (p.next !== null)
	    {
		p.next();
	    }
	}
    }
    
    var ChannelState = { idle : 0, awaitingRead : 1, awaitingWrite : 2 }
    
    //////
    // Process
    //////
    
    function Process() {
	this.commData = null;
    }

    Process.prototype.start = function() {
	schedule(this);
    }
    
    //////
    // Channel
    //////
    
    function Channel() {
	this.data = null;
	this.state = ChannelState.idle;
	this.waitingProcess = null;
    }
    
    Channel.prototype.write = function(p) {
	if (this.state == ChannelState.awaitingWrite) {
	    this.waitingProcess.commData = p.commData;
	    p.commData = null;
	    schedule(this.waitingProcess);
	    this.waitingProcess = null;
	    schedule(p);
	    this.state = ChannelState.idle;
	}
	else {
	    this.waitingProcess = p;
	    this.state = ChannelState.awaitingRead;
	}
    }
    
    Channel.prototype.read = function(p) {
	if (this.state == ChannelState.awaitingRead) {
	    p.commData = this.waitingProcess.commData;
	    this.waitingProcess.commData = null;
	    schedule(this.waitingProcess);
	    this.waitingProcess = null;
	    schedule(p);
	    this.state = ChannelState.idle;
	}
	else {
	    this.waitingProcess = p;
	    this.state = ChannelState.awaitingWrite;
	}
    }
    
    //////
    // Public interface
    //////
    
    return {
	helloWorld : function() { console.log("Hello, world."); },
	Process : Process,
	Channel : Channel,
	main : main
    };
}();

var c1 = new __RT.Channel();

////// Test process Recv, receives a value on c1
var Recv = function() {
    __RT.Process.call(this);
    this.a = 0;
    this.b = 0;
    this.next = Recv.prototype.slice0;
}
Recv.prototype = new __RT.Process();
Recv.prototype.constructor = Recv;

Recv.prototype.slice0 = function() {
    this.a += 1;
    this.next = Recv.prototype.slice1;
    c1.read(this);
}

Recv.prototype.slice1 = function() {
    this.a = this.commData;
    this.b = this.a * 2;
    this.next = null;
}
//////

////// Test process Send, sends a value on c1
var Send = function() {
    __RT.Process.call(this);
    this.next = Send.prototype.slice0;
}
Send.prototype = new __RT.Process();
Send.prototype.constructor = Send;

Send.prototype.slice0 = function() {
    this.commData = 999;
    this.next = null;
    c1.write(this);
}
//////

s = new Send();
r = new Recv();

// Schedule processes
s.start();
r.start();

__RT.main();

console.log(r.a);
console.log(r.b);

