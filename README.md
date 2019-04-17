# Mapping-Robot
A subsumption-architecture based robot built using the lejos ev3 API.
The task is to build an occupancy grid of the arena and display it on the client GUI and the robot LCD screen.

## Environment Configuration
- An arena of 7x6 cells in size, where each cell is 25x25cm.
- The arena will have a a number of obstacle blocks at random locations. These obstacles will be 25x25x10cm in dimension. 
- Each of the four corner squares will have a unique colour that can be detected as a beacon, to assist with localization.

## Robot Configuration
- A 4 wheeled lego car.
- Two bumper sensors at the front of the chasis.
- One gyro sensor in the middle (beneath) of the chasis.
- One ultrasound at the front of the car linked to a rotational motor.

## Network Configuration
- Server is to be ran on the robot.
- Client is to be ran on any other machine with networking capabilities.

## How to use this? 
Recommended way:
- Build a robot with the configuration described above.
- Build an arena with the configuration described above.
- Install Eclipse with lejos ev3 packages installed.
### (from eclipse)
- Place the robot at one of the corners of the arena.
- Run [core/Core.java](core/Core.java)
- Wait for the files to be uploaded to the robot.
- Run [network/Client.java](network/Client.java)
