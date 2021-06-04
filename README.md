# Dasware

## INSTALACIÓN

Conectar dos dipositivos móviles al ordenador, ejecutar en un dispositivo la app del periferico y en otro dispositivo la app BluetoothGatt.

##Requisitos

Dar permisos de ubicacion desde el dispositivo movil en ambos dispositivos. Activar el bluetooth y la ubicacion(IMPORTANTE: si la ubicación no está activada o la aplicación no tiene el permiso de ubicación activo NO FUNCIONARÁ).

##USO

El dispositivo en el que esté corriendo la app BluetoothGatt será el dispositivo central y el otro dispositivo donde esté corriendo la app Periférico será el dispositivo periferico.

- El central escanea los dispositivos bluetooth ble cercanos.(Si el nombre de tu dispositivo periferico no sale en el scaner prueba a cambiarle el nombre del bluetooth al dispositivo periferico por uno mas corto desde las opciones bluetooth del sistema, por ejemplo: Perif).

- Una vez escanee el el periferico pulsar en el nombre de este. Pasará a una nueva actividad donde se encuentran los botones Write characteristic y Read Characteristic.

- El write cambia el valor de la caracteristica a AA 30 78 41 41 o a BB 30 78 42 42 dependiendo del valor que tenga el periferico en ese momento(que es el que se muestra la app del periférico.

- El valor cambiado se muestra en la actividad del central(el cambio de valor es inmediato ) y en el periferico(EN EL PERIFERICO este valor cambiara cuando se produzca una modificacion en el sistema, ejemplo: cuando llegue un sms o cuando cambie el valor de la hora) Aunque internamente el valor ya está cambiado).  

##FUNCION

Con este pequeño ejemplo he aprendido lo necesario para manejar una app bluetooth LE. Crear un perfil, crear un servidor en un periférico, escanear dispositivos bluetooth, escribir y leer caracteristicas de un perfil.


