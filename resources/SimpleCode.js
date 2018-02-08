// Set up the socket
let url = "ws://" + location.host;
let socket = new WebSocket(url);

socket.onerror = function () { console.log("ERROR") };
socket.onclose = function () { console.log("SOCKET CLOSED") };
socket.onmessage = messageReceived;
socket.onopen = socketOpened;

function socketOpened () {
    console.log("SOCKET OPENED")
    // Payload Size of 125
    let msg125 = "Lorem ipsum dolor sit amet consectetur adipiscing elit, sagittis nam ad magna sociosqu turpis ac, risus blandit aenean ferme."
    // Payload Size of 126
    let msg126 = "Lorem ipsum dolor sit amet consectetur adipiscing elit pulvinar, bibendum morbi pharetra neque egestas curabitur. Et justo acd."
    // Payload Size of 255, (takes 1 byte to represent 255 -> 1111 1111)
    let msg255 = "Lorem ipsum dolor sit amet consectetur adipiscing elit aenean, posuere egestas\n\
ridiculus hendrerit nisi commodo luctus dictum fames, himenaeos metus eget ornare quam id montes.\n\
Fermentum torquent aptent dignissim enim rhoncus praesent, pharetra eu feugia."

    socket.send(msg255);
}

function messageReceived(event){
    // Prints the message that was recieved.
    console.log(event.data);
}