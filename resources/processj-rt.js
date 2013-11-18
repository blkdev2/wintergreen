__RT = function() {
    //////
    // Scheduler logic
    //////
    
    var processes = {};
    
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
    
    function Process(name) {
	this.name = name;
	this.commData = null;
	this.completionBarrier = null;
	// Register process
	processes[name] = this;
    }
    
    Process.prototype.start = function() {
	schedule(this);
    }
    
    Process.prototype.finish = function() {
	console.log("--finish() called for process " + this.name);
	// Unregister process
	delete processes[this.name];
	// Synchronize on completion barrier
	if (this.completionBarrier !== null) {
	    this.completionBarrier.sync(this);
	}
    }
    
    Process.prototype.toString = function() {
	return this.name;
    }
    
    Process.prototype.enrollOnCompletionBarrier = function(cb) {
	cb.enroll(this);
	this.completionBarrier = cb;
    }
    
    Process.prototype.instanceCounter = 0;
    
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
    // Barrier
    /////

    function Barrier() {
	this.enrolledProcs = {};
	this.nEnrolled = 0;
	this.waitingProcs = {};
	this.nWaiting = 0;
    }
    
    Barrier.prototype.enroll = function(p){
	this.enrolledProcs[p] = true;
	this.nEnrolled++;
    }
    
    Barrier.prototype.resign = function(p) {
	if (p in this.enrolledProcs) {
	    delete this.enrolledProcs[p];
	    this.nEnrolled--;
	}
    }

    Barrier.prototype.sync = function(p) {
	// If process is not enolled, this is an error.
	//console.log(this.enrolledProcs);
	//console.log(p.toString() in Object.keys(this.enrolledProcs));
	if (!(p in this.enrolledProcs)) {
	    return;
	}

	// Add process to wait list
	if (!(p in this.waitingProcs)) {
	    this.waitingProcs[p] = true;
	    this.nWaiting++;
	}
	
	// Check if barrier is completed
	if (this.nEnrolled === this.nWaiting) {
	    console.log("barrier completed");
	    // Allow all processes to continue
	    for (var pname in this.enrolledProcs) {
		//console.log(pname);
		schedule(processes[pname]);
	    }
	    // Clear wait list
	    this.waitingProcs = {};
	    this.nWaiting = 0;
	}
    }
    
    //////
    // Completion barrier
    /////
    function CompletionBarrier(resumeProc) {
	this.nEnrolled = 0;
	this.nCompleted = 0;
	this.resumeProc = resumeProc;
	this.enrolledProcs = {};
    }
    
    CompletionBarrier.prototype.enroll = function(p) {
	this.nEnrolled++;
	this.enrolledProcs[p.name] = true;
    }
    
    CompletionBarrier.prototype.sync = function(p) {
	if (!(p in this.enrolledProcs)) {
	    return;
	}
	
	console.log("CompletionBarrier: process " + p.name + "completed");
	
	this.nCompleted++;
	
	if (this.nEnrolled == this.nCompleted && this.resumeProc !== null) {
	    schedule(this.resumeProc);
	}
    }
    
    //////
    // Functions
    //////

    // runInParallel
    // First argument should be a process to resume after finishing the PAR.
    // The rest of the arguments 
    function runInParallel(resumeProc, processes) {	
	var cb = new CompletionBarrier(resumeProc);
	
	// For each process in the argument list,
	// set the completion barrier.
	for (var i = 0; i < processes.length; i++) {
	    var p = processes[i];
	    p.enrollOnCompletionBarrier(cb);
	}
	
	// Start the processes.
	for (var i = 0; i < processes.length; i++) {
	    var p = processes[i];
	    console.log("PAR: starting " + p.name);
	    p.start();
	}
    }
    
    //////
    // Public interface
    //////
    
    return {
	helloWorld : function() { console.log("Hello, world."); },
	Process : Process,
	Channel : Channel,
	Barrier : Barrier,
	runInParallel : runInParallel,
	main : main
    };
}();

var c1 = new __RT.Channel();
var b1 = new __RT.Barrier();

////// Test process Recv, receives a value on c1
var Recv = function() {
    __RT.Process.call(this, "Recv$" + (Recv.prototype.instanceCounter++));
    this.a = 0;
    this.b = 0;
    this.c = 0;
    this.next = Recv.prototype.slice0;
}
Recv.prototype = new __RT.Process();
Recv.prototype.constructor = Recv;
Recv.prototype.instanceCounter = 0;

Recv.prototype.slice0 = function() {
    this.next = Recv.prototype.slice1;
    b1.enroll(this);
    this.a += 1;
    c1.read(this);
}

Recv.prototype.slice1 = function() {
    this.next = Recv.prototype.slice2;
    this.a = this.commData;
    this.b = this.a * 2;
    b1.sync(this);
}

Recv.prototype.slice2 = function() {
    this.next = null;
    this.c = 555;
    this.finish();
}

//////

////// Test process Send, sends a value on c1
var Send = function() {
    __RT.Process.call(this, "Send$" + (Send.prototype.instanceCounter++));
    this.next = Send.prototype.slice0;
}
Send.prototype = new __RT.Process();
Send.prototype.constructor = Send;
Send.prototype.instanceCounter = 0;

Send.prototype.slice0 = function() {
    b1.enroll(this);
    this.commData = 999;
    this.next = Send.prototype.slice1;
    c1.write(this);
}

Send.prototype.slice1 = function() {
    this.next = Send.prototype.slice2;
    b1.sync(this);
}

Send.prototype.slice2 = function() {
    this.next = null;
    this.finish();
}

//////

////// Test process Main, runs Send and Recv in parallel
var Main = function() {
    __RT.Process.call(this, "Main$" + (Main.prototype.instanceCounter++));
    this.next = Main.prototype.slice0;
}
Main.prototype = new __RT.Process();
Main.prototype.constructor = Main;
Main.prototype.instanceCounter = 0;

Main.prototype.slice0 = function() {
    this.next = Main.prototype.slice1;
    this.S = new Send();
    this.R = new Recv();
    __RT.runInParallel(this, [this.S, this.R]);
}

Main.prototype.slice1 = function() {
    this.next = null;
    console.log("Main: PAR returned.");
    this.finish();
}

//////


//s = new Send();
//r = new Recv();

// Schedule processes
//s.start();
//r.start();

//__RT.runInParallel(null, [s, r]);

M = new Main();
M.start()

__RT.main();

console.log(M.R.a);
console.log(M.R.b);

console.log(M.R.c);
