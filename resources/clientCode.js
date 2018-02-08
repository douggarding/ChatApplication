//console.log()

// Set up the socket
let url = "ws://" + location.host;
let socket = new WebSocket(url);
socket.onmessage = messageReceived;

// Connects to a specific room on the server
function connectToRoom(){
    let roomToJoin = document.getElementById("room")[0].value;
    let joinRequest = "{\"message\":\"" + roomToJoin + "\",\"user\":\"join\"}";
    // Join the requested room
    socket.send(joinRequest);
}

// Sends a message to whichever room the user's logged into
function sendMessageToRoom(){
    let userName = document.getElementById("user")[0].value;
    let userMessage = document.getElementById("message")[0].value;
    let message = "{\"message\":\"" + userMessage +  "\",\"user\":\"" +  userName + "\"}";
    // send a message:
    socket.send(message);
}

function messageReceived(event){
    // Get the message from the server
    let jMessage = JSON.parse(event.data);
    // Turn the message into a paragraph element
    let newMessage = document.createElement("p");
    newMessage.innerHTML = jMessage.user + ": " + jMessage.message;
    // Insert the paragraph into the chat window
    let messageWindow = document.getElementById("chatWindow");
    messageWindow.appendChild(newMessage);
    console.log(newMessage);
}

