Solar System Simulator
===========
Experimenting with jME3 / Summer 2011

- High resolution imagary courtasy of NASA and others. 
- Planetary distances are proportional to each other.
- Planetary sizes are proportional to each other. The Sun had to be scaled down further due it being insanely massive.
- Planetary axial tilt is roughly accurate.
- Planetary oribits are roughly accurate to the date. The date can be increased/decreased via the ```f``` and ```r``` keys.
- Planetary rotation scaled to one Earth day per 30s.
- Atmosphere and night-lights are implemented for Earth.

Screen Shots: http://imgur.com/a/T6VLJ#0

Main class: 
> net.cofront.solarsystem.Main


Oribital calculations can be found in: 
> net.cofront.solarysystem.OrbitalElements

Controls Summary:
```
Left Mouse : Click to lock/unlock mouse aim
wsad  : Move camera forward/backward/left/right
q     : Move camera up
z     : Move camera down
shift : Hold down to move at 10% of normal speed
o     : Toggle planet orbital rings on/off
i     : Toggle planet indicators on/off
y     : Toggle milkyway skymap on/off
c     : Toggle clouds/atmosphere (Earth only)
f     : Increment the date
r     : Decrement the date
1 - 9 : Jump to planet
```


