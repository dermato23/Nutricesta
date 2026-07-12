import os
import json
import requests
from dotenv import load_dotenv

load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")

raw_text_mayo = """
Pedido No.: 651389
#
Fecha : 2026/05/30
Cajero
C:
Colombiana de Comercio S.A.
1 101
Factura Electronica de Venta: X2272572915
Telefo.: 3000000000
CR 68 No 72-43
Cliente: CLIENTE DE PRUEBA
Direcc.: CL 1 1
1 1 1 1 1 1 1
Barrio Ficticio
Telefono: (601)4073033
Forma de Pago: CONTADO
2 120
ALKOST0 AV. 68
Nit. 890900943-1
Email: cliente.prueba@ejemplo. com
vozcliente@alkosto.com. co
6
Medio de Pago: TARJETA DEBITO/CREDITO
Observa:
Articulo
ALKOST0
7 164
9
Cajero De PruebaLocal: 02
1000000001
Calabacin amarillo kg
Descuento 30,00 %
EXC 0 0,54
Calabacin Verde kg
escuento 30,00 %
3 7705946725149 EXC 0
Tomate chonto 1kg
4 7701023689038 EXC 0
Limon Tahiti 1kg
Descuento 40, 00 %
12 118
Habichuela kg
Descuento 40,00 %
8 222
1:
5 126
EXC 0 0,46
Cebolla Cabezona Roja kg
234
EXC 0 1,78
Platano Harton Verde kg
Descuento 40.00 %
Papaya Kg
Descuento 40,00 %
11 238
Remolacha kg
16 284
Pimenton Rojo kg
114
Banano Criollo kg
Descuento 40,02 %
10 208
Brocoli kg
13 7701023945424
Ajo Imp 90g
IVA Ipo CantTotal s/desc
EXC 0 0,36
Caja: 21
Hora: 09:47:07
Ahuyam in Kg
7705946725002
1
EXC 0 0,98
1
EXC 0 0,34
EXC 0 0,43
EXC 0 0,91
EXC 0
EXC 0 0,38
EXC 0 0,44
14 7702535011 799 19 0 1
Gaseosa CocaCola Zero 1.5L
Descuento 20,00 %
BOGOTA
15 7709514491467 EXC 0 1
Lech. Salanova Mora Hidro 140g
1
Uva Isabella 500g
Descuento 30,00 %
EXC o 0.84
EXC 0 1
18 7700934001069 EXC 0 1
Espinaca Tierna Bolsa 200g
1.620
486-
2.430
729-
6.900
5.900
2.360-
3.023
11.570
4.628-
9.702
3.881-
2.915
1.914
766-
5.307
2.123-
2.195
5.090
2.600
6.500
1.300-
5.550
2.268
8.900
2.670-
7.300
CUFE
f17de3162fee90b716b52b1b3882ec5b03ae659387a2
Oe3ef2d0baf68e3ccde596525376452142b794227a5c
REPRESENTACION GRAFICA DE LA FACTURA ELECTRONICA
Descuento 30,00 %
19 226
Pitahaya kg
Descuento 30,00 %
20 7702511002964 EXC 0 1
Lenteja Diana 1000g
Descuento 15,00 %
21 7705946241090 EXC 0
Cebolln Capuchn 50g
Descuento 40, 00 %
22 2400206000000 EXT 0 0,86
Carne Molida Light Res Bja kg
Descuento 30,00 %
23 2400198000000 EXT 0 0,95
Bota Posta de Res Fam Bja Kg
Descuento
to 30,00 %
24 7701023961585 19 0 1
Tortilla Alk Integral 8u 250g
25 7705946934633 EXC 0
Arandano 500g
Descuento 40,00 %
EXC 0 0,26
27
26 7709990139082 EXC 0
Zanahoria Buena Santerra 1000g
7707237230011 EXC 0 1
Descuento 30, 00 %
Tomate Cherry 500g
28 7702001121748
1
Ques Alpin Sabana Taj x25 400g
Descuento 25,00 %
34
29 7700149059800 19 0 1
Granola K Proteina 600g
Descuento 20,00 %
1
30 77073651 75284 19 0 1
1
Kefir Dejamu Semidescr Nat 1L
3* 7700149611251 EXC 0 1
Aguacate Hass 1kg
Descuento 30,00 %
32 7701023821513 19 0 1
Vinagre Blanco Alkosto 1000ml
0 1
33 7701023657136 19 0 1
Bol Bas Cas Alk Rec 65x80 20u
7701023533379 19 0 1
38 77020201 20036 5
Bol Bas Alk Verde 53x52 30u
35 7707335283490 EXC 0 1
Garbanzo Maritza 1000g
Descuento 20,00 %
36 7701101247266 19
40 7703092499884 5
Salchicha Ranchera x 480g
37 7705946171533 EXC 0 0 1
Frijol Bola Roja Alkosto 1K
41 7702032107933 5
42 023100119335
Cabello Angel La Mueca 1000g
39 7707304621100
0 2
Huevo Kikes Rojo AA Bandja 30u
Descuento 15, 00 %
1
1
Choc Girones Leche Stev 125gx2
Descuento 50,00 %
CUFE
1
Cafe Sello Rojo Trad 600g x2u
Descuento 20, 00 %
1
5 0 1
Snacks Gat Temp Camaron 180g
43 77021290751 14 19 0 1
Jamon
ccionad Colanta 450g
Descuento 30.00 %
44 7707108070760 5
1
Alim Hum Whiskas Adult 85gx1 2
Descuento 10,00 %
45 7702889001989 19 0 1
Vinagreta Aderezos Light 1005g
2.190-
4.680
1.404-
6.800
1.020-
2.950
1.180-
31.882
9.565-
40.545
12.164-
5.700
31.000
12.400-
5.000
3.950
2.685-
31.800
7.950-
17.900
3.580-
21. 100
8.500
2.550-
2.900
6.750
4.900
6.900
1.380-
25.400
14.900
6.600
41.900
6.286-
21.800
10.900-
61.900
12.380-
24.900
18.950
5.685-
31.900
3.190-
24.900
f17de3162fee90b716b52b1b3882ec5b03ae659387a2
Oe3ef2d0baf68e3ccde596525376452142b794227a5c
REPRESENTACION GRAFICA DE LA FACTURA ELECTRONICA
Descuento 40,00
46 7702031955467 19 0 1
Enj Lister in Cool Mint 500mlx3
Descuento 40,00 %
47 7701023539258 19 0 1
Atun Alkosto Aceite 170g x6
Descuento 20, 00 %
48 7702560030888
Cep Fluocardent Limp Medio x2
Descuento 20, 00 %
49 7702085243480 5
Fusilli Monticello 500g
50 7703133010511 19 0
MegaPack Yupi x30u 594g
Descuento 30, 00 %
51 7702177161043 EXC 0 1
52 7701023223713 19
Crema Alque PrepCalien 170g 3u
Descuento 20,00 %
53 7702084137520 5
55
Harina Pan Blanca x 1000g
Salsa Bolognes Alkosto 50g x2u
0 1
54 7701023814805 19 0 1
Bol Bas Apto Alkosto 51x70 30u
7702097159137 19 0 1
56 770212900496 1
19
57 7702511637975 EXC 0
Sals Pic Amazon Sw/Must 355ml
Descuento 25, 00 %
Maiz Pira Diana 1000g
Descuento 15, 00 %
58 7509552961478
Agua Brisa Bidon
Descuento 20,00 %
L Colanta Entera LV Bls 1L x6u
Descuento 20,00 %
60 7702010382079 19
Subtotal
Valor IVA
Valor Total
Sh Elvive Dream Liso 68 Oml
Descuento 40,00 %
59 7702535002650 EXC 0 2
REDEBAN 00
REDEBAN 00
Tarifa IVA
EXT
19
EXO
19
1
Lavaloz Axion Lig Lim Dp 1.5L
Descuento 30.00 %
5
6 Lts
61 7705326076038 EXC 0 1
Pan Taj Bimbo Vit Gra/Ara 500g
62 7702120014310 19 0 1
Reclame 2 tiquetes.
1
PH Rosal UltraConf XG 25Mt x30
Descuento 30,00 %
Total lineas factura: 62
1
$
1
Vr-Base
50.698
172.752
251.607
1
125.648
1
1
1
Responsable I.V.A.
RETENEDORES DE IVA
Autorretenedores de Renta
9.960-
23.960-
59.900
29.900
5.980-
11.900
2.380-
Somos Grandes Contribuyentes
Resoluc. 000200 Dic. 27 de 2024
7.700
Res.No. 0008327 Ago. 24 2.010.
24.900
7.470-
14.800
2.960-
5.700
3.600
7.350
14.900
3.725-
30.800
6.160-
5.200
780-
42.900
17.160-
16.600
3.320-
19.900
5.970-
10.800
48.900
14.670-
684.807
738.894
54.085
250.000
488.894
Vr-IVA
0
0
47.803
6.282
Autorretenedor ICA - Resolucion 132 de 1997
Numeracion Autorizada ylo Habilitada por la
DIAN
Prefijo: X227 del No. 2500001 al 4000000
CUFE
f17de3162fee90b716b52b1b3882ec5b03ae659387a2
Oe3ef2d0baf68e3ccde596525376452142b794227a5c
REPRESENTACION GRAFICA DE LA FACTURA ELECTRONICA
Numero de Formulario 18764078184539
del 2024/08/28 al 2026/08/28
Estimado Cliente, consulta la
politica de cambios en:
www.alkosto. com/politicas
Icambios - devoluciones /clcambios -devoluciones
Software e de faCturacion electrontcd ro
eBill, fabricado por EYM
OLOGY S.A.S,
Nit 900306823-4 Proveedor tecnologico : FYM
TECHNOL0GY S.A.S
USTED SE AHORRO
$219.947
Adicionales a los ahorros que
Siempre tiene en
2026/05/30
CUFE
f17de3162fee90b716b52b1b3882ec5b03ae659387a2
Oe3ef2d0baf68e3ccde596525376452142b794227a5c
REPRESENTACION GRAFICA DE LA FACTURA ELECTRONICA
"""

prompt_v2 = """
Eres un asistente experto en analizar recibos de compra y clasificar productos.
El texto crudo (RAW) del OCR proviene de un recibo largo de supermercado donde el escaneo a veces separa los nombres de los productos de sus precios correspondientes en bloques distintos, o lista los precios más abajo en el mismo orden que aparecen los artículos.

Tu tarea es:
1. Reconstruir mentalmente el listado de compras asociando cada producto con su precio unitario/neto real.
   - Analiza el orden de los productos en las secciones superiores y compáralo con los bloques de precios que aparecen más abajo.
   - Ten en cuenta que los valores como "Descuento 30.00 %" o "9.960-" restan del precio del producto inmediatamente anterior.
   - El precio neto de cada producto es su precio base menos los descuentos aplicables.
2. Agrupar y clasificar todos los artículos comestibles y no comestibles en los siguientes 12 grupos autorizados, sumando el gasto total neto (en pesos colombianos, COP) en cada categoría:
   - "Proteina": Proteínas animales (carnes, pollo, de res, cerdo, pescado, jamón, salchichas, atún, huevos). NO lacteos.
   - "Verduras": Verduras, hortalizas, calabacín, tomate, cebolla, habichuela, etc.
   - "Frutas": Papaya, arándanos, uva, pitahaya, banano, etc.
   - "Lacteos": Leche, queso, yogures, mantequilla, kéfir, crema.
   - "Carbohidratos": Pan, tortillas, pastas, fusilli, harina pan, arroz.
   - "Cereales": Granola, avena, cereales de desayuno.
   - "Granos": Frijol, garbanzo, lentejas.
   - "AseoPersonal": Cepillo dental, enjuague bucal, desodorante, champú, jabón corporal.
   - "AseoHogar": Detergente, bolsas de basura, lavalozas.
   - "Snacks": Papas fritas, chocolates, pasabocas, gaseosa, snacks dulces/salados (excepto comida para mascotas).
   - "Mascotas": Alimento para perros/gatos (ej. Whiskas, Snacks Gat Temp Camaron).
   - "Grasas": Aguacate, aceites, frutos secos.

3. Extraer el valor total exacto de la factura ("TotalFactura"), que en este recibo está impreso como "738894" (o similar al total de la compra).
4. Extraer el descuento/ahorro total de la factura ("AhorroTotal"), impreso como "$219.947" (debe ser 219947).
5. Evaluar la salud del mercado en una escala de 1 a 10 ("PuntajeSaludable") y proveer un motivo conciso ("MotivoSaludable").
6. Estimar rigurosamente los macronutrientes totales en GRAMOS de la compra en base a los pesos impresos de los alimentos (ej. "Granola K Proteina 600g" -> 600g de producto, "Atun 170g x6" -> 1020g, etc.) y su composición promedio de macronutrientes:
   - "Proteina_g": Gramos de proteína totales aproximados.
   - "Carbohidratos_g": Gramos de carbohidratos totales aproximados.
   - "Grasas_g": Gramos de grasas totales aproximados.
   (Calcula de manera real basada en la lista de compras del recibo, no devuelvas 0!).

Devuelve UNICAMENTE un objeto JSON plano con las siguientes llaves exactas:
"TotalFactura", "AhorroTotal", "PuntajeSaludable", "MotivoSaludable", "FechaFactura", "Proteina", "Verduras", "Frutas", "Lacteos", "Carbohidratos", "Cereales", "Granos", "AseoPersonal", "AseoHogar", "Snacks", "Mascotas", "Grasas", "Proteina_g", "Carbohidratos_g", "Grasas_g"

IMPORTANTE: Para todos los valores monetarios de las categorías y totales, escribe solo números enteros limpios, sin puntos ni comas (ej. 738894).
Para la fecha, devuélvela en formato "YYYY-MM-DD" (FechaFactura).

Texto del recibo a analizar:
{raw_text}
"""

def test_model_prompt(model_name, prompt):
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent"
    headers = {'Content-Type': 'application/json', 'x-goog-api-key': api_key}
    data = {
        "contents": [{"parts": [{"text": prompt.replace("{raw_text}", raw_text_mayo)}]}],
        "generationConfig": {
            "temperature": 0.1,
            "responseMimeType": "application/json"
        }
    }
    response = requests.post(url, headers=headers, json=data)
    print(f"Status {model_name}: {response.status_code}")
    if response.status_code == 200:
        print(response.json()["candidates"][0]["content"]["parts"][0]["text"])
    else:
        print(response.text)

print("--- Testing gemini-2.5-flash-lite ---")
test_model_prompt("gemini-2.5-flash-lite", prompt_v2)

print("\n--- Testing gemini-2.5-flash ---")
test_model_prompt("gemini-2.5-flash", prompt_v2)
