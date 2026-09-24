# Complementary Reimagined — integração GafiShader

O Complementary Reimagined expõe muitas opções configuráveis no shader pack. Esta lista é usada pelo comando /gafishader complementary.

## Atmosphere

Opções relevantes incluem:

- NIGHT_STAR_AMOUNT
- AURORA_STYLE_DEFINE
- AURORA_CONDITION
- NIGHT_NEBULAE
- NIGHT_NEBULA_I
- RAINBOWS
- RAINBOW_STYLE_DEFINE

A documentação do pack descreve Aurora com vários estilos/condições e rainbows com condições como after-rain ou always.

## Clouds

- CLOUD_STYLE_DEFINE
- CLOUD_ALT1
- CLOUD_SPEED_MULT
- CLOUD_SHADOWS
- CLOUD_UNBOUND_AMOUNT
- CLOUD_UNBOUND_SIZE_MULT
- DOUBLE_REIM_CLOUDS
- CLOUD_ALT2

## Fog / Light shafts

- BORDER_FOG
- CAVE_FOG
- ATM_FOG_MULT
- ATM_FOG_DISTANCE
- ATM_FOG_ALTITUDE
- LIGHTSHAFT_BEHAVIOUR
- LIGHTSHAFT_SMOKE
- LIGHTSHAFT_SUNSET_SATURATION

## Sun / Moon

- SUN_MOON_STYLE_DEFINE
- SUN_ANGLE
- SUN_MOON_HORIZON
- SUN_MOON_DURING_RAIN

## Weather

- RAIN_STYLE
- SPECIAL_BIOME_WEATHER
- WEATHER_TEX_OPACITY
- IMPROVED_RAIN_DEFINE

## Water

- WATER_STYLE_DEFINE
- WATER_CAUSTIC_STYLE_DEFINE
- WATER_ALPHA_MULT
- WATER_FOG_MULT
- WATER_FOAM_I
- WATER_REFRACTION_INTENSITY
- WAVING_WATER_VERTEX

## Camera

- BLOOM
- VIGNETTE
- MOTION_BLUR
- LENS_FLARE

## Color

- EXPOSURE
- CONTRAST
- SATURATION
- VIBRANCE
- color grading

## Porque nem tudo é um comando direto

No estado atual, o Iris expõe publicamente a API para ativar/desativar shaders, aplicar essa mudança e abrir as configurações. Não expõe um setter público estável para cada opção arbitrary do shader pack.

Assim, /gafishader complementary group ... mostra a informação real e /gafishader complementary settings abre a configuração oficial, enquanto tempo/clima são controlados diretamente no cliente.
