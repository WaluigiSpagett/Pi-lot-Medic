from pymavlink import mavutil
import math
import time

# Connect to the vehicle
connection = mavutil.mavlink_connection('udp:127.0.0.1:14550')
connection.wait_heartbeat()
print("Connected to the vehicle!")

# Constants
GRAVITY = 9.81  # m/s²

def get_telemetry():
    """Fetches altitude, ground speed, and coordinates."""
    msg = connection.recv_match(type='GLOBAL_POSITION_INT', blocking=True)
    altitude = msg.relative_alt / 1000.0  # Convert from mm to meters
    speed_msg = connection.recv_match(type='VFR_HUD', blocking=True)
    speed = speed_msg.groundspeed  # Speed in m/s
    return altitude, speed

def calculate_drop_distance(altitude, speed):
    """Calculates horizontal distance needed for drop."""
    fall_time = math.sqrt((2 * altitude) / GRAVITY)
    drop_distance = speed * fall_time
    return drop_distance

def activate_drop():
    altitude, speed = get_telemetry()
    print(f"Altitude: {altitude} m, Speed: {speed} m/s")

    drop_distance = calculate_drop_distance(altitude, speed)
    print(f"Calculated drop distance: {drop_distance:.2f} meters")

    # Calculate current position and distance to waypoint
    wp_msg = connection.recv_match(type='MISSION_ITEM', blocking=True)
    target_lat = wp_msg.x
    target_lon = wp_msg.y

    # Trigger the drop at the correct distance from the target
    while True:
        position = connection.recv_match(type='GLOBAL_POSITION_INT', blocking=True)
        current_lat = position.lat / 1e7
        current_lon = position.lon / 1e7

        # Calculate distance to target waypoint
        distance_to_target = mavutil.mavlink.distance_two(current_lat, current_lon, target_lat, target_lon)
        print(f"Distance to target: {distance_to_target:.2f} meters")

        if distance_to_target <= drop_distance:
            # Activate the drop
            connection.mav.command_long_send(
                connection.target_system,
                connection.target_component,
                mavutil.mavlink.MAV_CMD_DO_SET_RELAY,
                0,
                0,  # Relay number (0 for first relay)
                1,  # Activate
                0, 0, 0, 0, 0
            )
            print("Drop activated!")

            time.sleep(1)  # Keep relay active for a second

            # Deactivate relay
            connection.mav.command_long_send(
                connection.target_system,
                connection.target_component,
                mavutil.mavlink.MAV_CMD_DO_SET_RELAY,
                0,
                0,
                0,  # Deactivate
                0, 0, 0, 0, 0
            )
            print("Drop deactivated!")
            break

        time.sleep(0.5)

activate_drop()
