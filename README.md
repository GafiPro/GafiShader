# GafiShader

**GafiShader** é um client mod Fabric para controlar localmente o tempo, clima e o Iris/Complementary Reimagined através de comandos.

O módulo foi desenhado para o projeto GafiScript, mas é deliberadamente **100% client-side**: não envia os comandos para o servidor e não precisa de permissões de operador para aplicar os overrides visuais locais.

## Compatibilidade

- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.19.x
- Fabric API 0.141.x
- Iris opcional; necessário para os comandos de toggle/settings/reload do shader
- Pensado especialmente para Complementary Reimagined

## Comandos

Todos os comandos funcionam em /gafishader. Também existe o alias /gafi.

### Shader / Iris

- /gafishader toggle
- /gafishader on
- /gafishader off
- /gafishader settings
- /gafishader gui
- /gafishader reload
- /gafishader status

### Tempo

Sem duração, o override fica ativo até normal/reset/disconnect.

- /gafishader day [duração]
- /gafishader alwaysday [duração]
- /gafishader dawn [duração]
- /gafishader noon [duração]
- /gafishader sunset [duração]
- /gafishader night [duração]
- /gafishader alwaysnight [duração]
- /gafishader midnight [duração]
- /gafishader freeze [duração]

Árvore avançada:

- /gafishader time day <duração>
- /gafishader time set <0-23999> [duração]
- /gafishader time speed <0-100> [duração]
- /gafishader time freeze [duração]
- /gafishader time unfreeze
- /gafishader time normal
- /gafishader time status

O speed 0 é equivalente a freeze. speed 0.5 avança devagar e speed 2 avança duas vezes mais depressa.

### Clima

- /gafishader clear [duração]
- /gafishader alwaysclear [duração]
- /gafishader rain [duração]
- /gafishader alwaysrain [duração]
- /gafishader snow [duração]
- /gafishader alwayssnow [duração]
- /gafishader storm [duração]
- /gafishader thunder [duração]

Árvore avançada:

- /gafishader weather clear [duração]
- /gafishader weather rain [duração]
- /gafishader weather snow [duração]
- /gafishader weather storm [duração]
- /gafishader weather thunder [duração]
- /gafishader weather intensity <chuva 0-1> <trovoada 0-1> [duração]
- /gafishader weather freeze [duração]
- /gafishader weather unfreeze
- /gafishader weather normal
- /gafishader weather status

### Aurora

- /gafishader aurora
- /gafishader aurora force [duração]
- /gafishader aurora night [duração]
- /gafishader aurora fullmoon [duração]
- /gafishader aurora status
- /gafishader aurora release

O setup de aurora força localmente a noite/lua cheia e céu limpo. O shader continua a decidir se desenha a aurora com base no próprio AURORA_CONDITION.

### Presets

- /gafishader preset sunny [duração]
- /gafishader preset night [duração]
- /gafishader preset sunset [duração]
- /gafishader preset storm [duração]
- /gafishader preset aurora [duração]

### Complementary

- /gafishader complementary
- /gafishader complementary list
- /gafishader complementary settings
- /gafishader complementary group atmosphere
- /gafishader complementary group clouds
- /gafishader complementary group fog
- /gafishader complementary group sunmoon
- /gafishader complementary group weather
- /gafishader complementary group water
- /gafishader complementary group materials
- /gafishader complementary group camera
- /gafishader complementary group color
- /gafishader complementary group dimensions

A catalogação usa opções documentadas do Complementary Reimagined, incluindo Aurora, night nebulae, rainbows, clouds, fog, sun/moon, weather, water, PBR, bloom, motion blur e color grading.

## Durações

O parser aceita:

- 30s
- 1m
- 5m
- 1h
- 1d
- 200t
- 1h30m
- forever

As durações são contadas em ticks do cliente, por isso continuam a decorrer mesmo se o tempo do mundo estiver congelado.

## Limitação do Iris

A API pública atual do Iris permite controlar o estado dos shaders e abrir a interface de configuração, mas não disponibiliza um setter público e estável para cada opção arbitrária de um shader pack.

Por isso o mod separa claramente:
1. controlo real do Iris através da API pública;
2. controlo client-side do estado Minecraft que o Complementary já usa para produzir os efeitos;
3. catalogação das opções específicas do Complementary e acesso rápido à sua interface.

O projeto não tenta mascarar internals instáveis do Iris como se fossem uma API oficial.

## Build

~~~bash
gradle clean build --no-daemon --max-workers=1
~~~

O JAR final aparece em build/libs/.

### Efeitos do shader — autocomplete completo

Existe agora uma árvore genérica:

- /gafishader effect

Ao abrir o argumento seguinte no autocomplete, aparecem as opções de configuração do Complementary Reimagined catalogadas a partir do shaders.properties atual, incluindo atmosfera, Aurora, nebulosas, arco-íris, nuvens, fog, light shafts, água, PBR, materiais emissivos, iluminação, câmera, tonemapping, TAA/FXAA, Nether, End, outlines e restantes definições expostas pelo menu do pack.

Exemplos:

- /gafishader effect AURORA_STYLE_DEFINE 1
- /gafishader effect AURORA_CONDITION 2
- /gafishader effect NIGHT_NEBULAE on
- /gafishader effect RAINBOWS on
- /gafishader effect BLOOM_ENABLED on
- /gafishader effect TM_EXPOSURE 0.5
- /gafishader effect CLOUD_SPEED_MULT 2
- /gafishader effect WATER_REFRACTION_INTENSITY 1

Também há autocomplete genérico de valores (on, off, true, false, números comuns, reimagined, unbound, after_rain, fullmoon, etc.); valores específicos não incluídos nesse pequeno conjunto podem continuar a ser escritos manualmente.

O comando usa o mecanismo de opções do Iris para colocar a alteração em fila e reaplicar o shader, em vez de editar o conteúdo do shader pack. A documentação do Iris confirma que as opções do shader pack são definidas através de shaders.properties e que as opções do menu correspondem às opções reconhecidas pelo próprio Iris.
