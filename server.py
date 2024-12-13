import socket
import pynput
import time
import json
from pynput.mouse import Controller

# Initialize the mouse controller
mouse = Controller()

# Display current pointer position
print(f"The current pointer position is {mouse.position}")

# Set up the server parameters
HOST = '192.168.1.37'  # Replace with your server's IP address
PORT = 8000            # Port matching the Android app

# Create a socket object
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# Bind the socket to the specified host and port
server_socket.bind((HOST, PORT))

# Start listening for connections (allow up to 1 client at a time)
server_socket.listen(1)

print(f"Server is listening on {HOST}:{PORT}...")
scalingfactor = 10  # Scaling factor for sensor data to mouse movement

# Function to parse the simplified JSON data (only x_delta, y_delta)
def unjsonify(received_json):
    x_delta = received_json["x_delta"]
    y_delta = received_json["y_delta"]
    return x_delta, y_delta

# Placeholder for previous mouse position
prev_x, prev_y = mouse.position

# Function to process and move the mouse based on the received deltas
def move_mouse(x_delta, y_delta):
    global prev_x, prev_y

    # Apply scaling factor to deltas for desired sensitivity
    dx = x_delta * scalingfactor
    dy = y_delta * scalingfactor

    # Adjust the mouse position based on the deltas
    new_x = prev_x + dx
    new_y = prev_y + dy
    
    # Ensure mouse position is within screen bounds (assuming screen size 1920x1080)
    screen_width = 1920
    screen_height = 1080
    new_x = max(0, min(new_x, screen_width))
    new_y = max(0, min(new_y, screen_height))
    
    # Update the mouse position
    mouse.position = (new_x, new_y)
    
    # Update previous position
    prev_x, prev_y = new_x, new_y

    print(f"Mouse moved to: {mouse.position}")

# Main server loop to accept connections and process sensor data
while True:
    # Accept a client connection
    conn, addr = server_socket.accept()
    print(f"Connected by {addr}")
    
    try:
        while True:
            # Receive data in chunks of 1024 bytes
            data = conn.recv(1024)
            
            if data:
                try:
                    # Decode and print the received data (x and y deltas)
                    received_data = data.decode('utf-8')
                    print(f"Received data: {received_data}")
                
                    # Unjsonify the received data (x_delta, y_delta)
                    sensor_data = json.loads(received_data)
                    x_delta, y_delta = unjsonify(sensor_data)
                    print(f"Movement Deltas: x_delta={x_delta}, y_delta={y_delta}")
                    
                    # Move the mouse based on the deltas
                    move_mouse(x_delta, y_delta)
                
                except Exception as e:
                    print(f"Error processing data: {e}")
                    continue
            else:
                print("No data received. Client may have disconnected.")
                break  # Break the inner loop to accept a new connection

    except Exception as e:
        print(f"Error: {e}")
    except KeyboardInterrupt:
        print("Server stopped by the user.")
        break
    finally:
        # Clean up the connection
        conn.close()
        print("Client disconnected. Waiting for new connection.")
        # Reset the mouse position to a default value
        mouse.position = (767.5, 431.5)
        prev_x, prev_y = mouse.position  # Reset previous position
