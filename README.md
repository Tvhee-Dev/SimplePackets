# SimplePackets

## Usage
1. To start using packets, create a new field in your plugin's main
```
private final SimplePackets simplePackets = new SimplePackets();
```
2. Implement these methods in onLoad, onEnable and onDisable:
```
@Override
pubic void onLoad()
{
        simplePackets.onPluginLoad();
}

@Override
pubic void onEnable()
{
        simplePackets.onPluginEnable(this);
}

@Override
pubic void onDisable()
{
        simplePackets.onPluginDisable();
}
```
3. Create a new class extending `PacketHandler`
```
public class MyPacketHandler extends PacketHandler
{
        @Override
        public void onSend(SimplePacket packet)
        {
                 //Your code here (called if the server sends a packet to the client)
        }
        
        @Override
        public void onReceive(SimplePacket packet)
        {
                 //Your code here (called if the client sends a packet to the server)
        }
}
```
4. Register your class in onEnable
```
simplePackets.registerHandler(new MyPacketHandler());
```

## Important
1. Do not forget to install `SimpleReflection`: https://github.com/Tvhee-Dev/SimpleReflection. This library depends on it!
