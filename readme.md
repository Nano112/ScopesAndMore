# ScopesAndMore

A Minecraft plugin for creating redstone monitoring probes and visualizing their values in various ways.

## Features

- Create probes to monitor redstone signals
- Group probes together to interpret multi-bit values
- Different interpretation modes (Binary, Hex, ASCII, Float, Signed/Unsigned Int)
- Value scopes to visualize probe group values
- Time series scopes to track value changes over time
- Customizable display settings
- In-game visualization using block displays

## Installation

1. Download the latest release from the releases page
2. Place the .jar file in your server's `plugins` folder
3. Restart your server

## Usage

### Basic Commands

#### Probe Management
- `/probe create [name]` - Create a probe at your target block
- `/probe list` - List all probes
- `/probe remove` - Remove a probe (look at it or specify ID)

#### Group Management
- `/probe group create <name>` - Create a new group
- `/probe group add <groupId> [probeId]` - Add a probe to group
- `/probe group remove <groupId> [probeId]` - Remove a probe from group
- `/probe group list` - List all groups
- `/probe group value <groupId>` - Get group value

#### Group Configuration
- `/probe group format <groupId> <format>` - Set group format
   - Available formats: binary, hex, ascii, float, uint, int
- `/probe group signal <groupId> <binary|hex>` - Set signal type
- `/probe group endian <groupId> <big|little>` - Set endianness
- `/probe group readorder <groupId> <msb|lsb>` - Set bit read order

#### Scope Management
- `/probe scope create <groupId>` - Create a value scope
- `/probe scope remove <scopeId>` - Remove a scope
- `/probe scope list` - List all scopes

### Creating a Basic Setup

1. Place your redstone components
2. Look at a redstone component and use `/probe create` to create a probe
3. Create a group: `/probe group create mygroup`
4. Add the probe to the group: `/probe group add mygroup`
5. Create a scope to visualize the value: `/probe scope create mygroup`

### Advanced Usage

#### Multi-bit Values
You can create groups of probes to read multi-bit values:

1. Create multiple probes on your redstone inputs
2. Add them to a group
3. Configure the group's format and reading order
4. Create a scope to visualize the combined value

Example for reading 8-bit ASCII:
```
/probe group create ascii_reader
/probe group add ascii_reader  # Add 8 probes
/probe group format ascii_reader ascii
/probe group readorder ascii_reader msb
/probe scope create ascii_reader
```

#### Time Series Monitoring
Create a time series scope to track value changes:
```
/probe scope create mygroup --type timeseries
```

### Configuration Options

#### Signal Types
- `BINARY` - Treats input as on/off (0 = off, >0 = on)
- `HEX` - Uses full 0-15 value from redstone power level

#### Interpreters
- `Binary` - Raw binary representation
- `Hex` - Hexadecimal representation
- `ASCII` - ASCII text (8 bits per character)
- `Float` - IEEE 754 floating-point (32 bits)
- `Unsigned Int` - Unsigned integer value
- `Signed Int` - Signed integer value (two's complement)

#### Endianness
- `BIG` - Most significant byte first
- `LITTLE` - Least significant byte first

#### Read Order
- `MSB_FIRST` - Most significant bit first (leftmost)
- `LSB_FIRST` - Least significant bit first (rightmost)

## Examples

### Creating a Hex Display
```
# Create probes for 4 redstone inputs
/probe create
/probe create
/probe create
/probe create

# Create and configure group
/probe group create hex_display
/probe group add hex_display  # Add all probes
/probe group format hex_display hex

# Create visualization
/probe scope create hex_display
```

### ASCII Text Reader
```
# Create 8 probes for 8-bit ASCII
/probe create input1
/probe create input2
# ... create all 8 probes ...

# Set up ASCII group
/probe group create ascii_reader
/probe group add ascii_reader  # Add all probes
/probe group format ascii_reader ascii
/probe group endian ascii_reader big

# Create scope
/probe scope create ascii_reader
```

## Contributing

Feel free to submit issues and pull requests with improvements!
