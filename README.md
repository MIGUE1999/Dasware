# Dasware

## INSTALACIÓN

Conectar dos dipositivos móviles al ordenador, ejecutar en un dispositivo la app del periferico y en otro dispositivo la app BluetoothGatt.

## Requisitos

Dar permisos de ubicacion desde el dispositivo movil en ambos dispositivos. Activar el bluetooth y la ubicacion(IMPORTANTE: si la ubicación no está activada o la aplicación no tiene el permiso de ubicación activo NO FUNCIONARÁ).

## USO

El dispositivo en el que esté corriendo la app BluetoothGatt será el dispositivo central y el otro dispositivo donde esté corriendo la app Periférico será el dispositivo periferico.

- El central escanea los dispositivos bluetooth ble cercanos.(Si el nombre de tu dispositivo periferico no sale en el scaner prueba a cambiarle el nombre del bluetooth al dispositivo periferico por uno mas corto desde las opciones bluetooth del sistema, por ejemplo: Perif).


![Cambio Nombre Bluetooth](https://github.com/MIGUE1999/Dasware/blob/main/Multimedia/WhatsApp%20Image%202021-06-04%20at%2015.09.48.jpeg)

![Cambio Nombre Bluetooth](https://github.com/MIGUE1999/Dasware/blob/main/Multimedia/WhatsApp%20Image%202021-06-04%20at%2015.09.48%20(1).jpeg)


![Scanner](https://github.com/MIGUE1999/Dasware/blob/main/Multimedia/WhatsApp%20Image%202021-06-04%20at%2015.12.41%20(1).jpeg)

- Una vez escanee el el periferico pulsar en el nombre de este. Pasará a una nueva actividad donde se encuentran los botones Write characteristic y Read Characteristic.

![Cambio Actividad](https://github.com/MIGUE1999/Dasware/blob/main/Multimedia/WhatsApp%20Image%202021-06-04%20at%2015.12.41.jpeg)

- El write cambia el valor de la caracteristica a AA 30 78 41 41 o a BB 30 78 42 42 dependiendo del valor que tenga el periferico en ese momento(que es el que se muestra la app del periférico.(El read va implementado en el write para mostrar el valor de la characteristica debajo del boton read characteristic).

![Write ](https://github.com/MIGUE1999/Dasware/blob/main/Multimedia/WhatsApp%20Image%202021-06-04%20at%2015.12.40.jpeg)

![Write ](https://github.com/MIGUE1999/Dasware/blob/main/Multimedia/WhatsApp%20Image%202021-06-04%20at%2015.12.40%20(1).jpeg)


- El valor cambiado se muestra en la actividad del central(el cambio de valor es inmediato ) y en el periferico(EN EL PERIFERICO este valor cambiara cuando se produzca una modificacion en el sistema, ejemplo: cuando llegue un sms o cuando cambie el valor de la hora) Aunque internamente el valor ya está cambiado).  

![Periferico ](https://github.com/MIGUE1999/Dasware/blob/main/Multimedia/WhatsApp%20Image%202021-06-04%20at%2015.12.41%20(2).jpeg)


## FUNCION

Con este pequeño ejemplo he aprendido lo necesario para manejar una app bluetooth LE. Crear un perfil, crear un servidor en un periférico, escanear dispositivos bluetooth, escribir y leer caracteristicas de un perfil.


