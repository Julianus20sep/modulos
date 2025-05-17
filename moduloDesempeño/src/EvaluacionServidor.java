import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.net.URLDecoder;

public class EvaluacionServidor {
    // Configuración básica del servidor
    // ServerSocket que nos permitirá manipular la conexión desde el lado del servidor
    private static final int PUERTO = 8080; // Puerto donde correrá el servidor
    private static final String CARPETA_WEB = "web"; // Carpeta con archivos CSS e imágenes

    // Almacenamiento de las evaluaciones
    private static Map<Integer, int[][]> evaluaciones = new HashMap<>();
    /*
     * Estructura de almacenamiento:
     * - Key: ID del profesor (1, 2 o 3)
     * - Value: Array bidimensional con las respuestas:
     *   [0] = Metodología (5 preguntas)
     *   [1] = Comunicación (3 preguntas)
     *   [2] = Evaluación (3 preguntas)
     *   [3] = Actitudes (3 preguntas)
     *   [4] = Valoración (1 pregunta)
     */

    // HTML básico de la página (parte inicial)
    private static final String HTML_INICIO = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="/style.css">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/charts.css/dist/charts.min.css">
            <title>Evaluación Docente</title>
        </head>
        <body>
            <div class="container">
                <div class="parte-1">
                    <img src="/img/logo.png" alt="Logo"  >
                </div>
                <div class="parte-2">
                    <div class="card">
                        <div class="card-img">
                            <img src="/img/profesor.jpg" alt="Profesor 1">
                        </div>
                        <div class="card-content">
                            <h3 class="card-title">Jaime Zapata</h3>
                            <p class="card-text">Metodologías Ágiles</p>
                            <a class="btn-revisar" href="/evaluar?profesor=1">Revisar</a>
                        </div>
                    </div>
                    <div class="card">
                        <div class="card-img">
                            <img src="/img/profesor.jpg" alt="Profesor 2">
                        </div>
                        <div class="card-content">
                            <h3 class="card-title">Jaime Zapata</h3>
                            <p class="card-text">Lógica de Programación</p>
                            <a class="btn-revisar" href="/evaluar?profesor=2">Revisar</a>
                        </div>
                    </div>
                    <div class="card">
                        <div class="card-img">
                            <img src="/img/profesor.jpg" alt="Profesor 3">
                        </div>
                        <div class="card-content">
                            <h3 class="card-title">Luis Zapata</h3>
                            <p class="card-text">Introducción a la programación</p>
                            <a class="btn-revisar" href="/evaluar?profesor=3">Revisar</a>
                        </div>
                    </div>
                </div>
                <div class="parte-3">
                    <img src="/img/logo.png" alt="Logo"  >
                </div>
        """;

    // HTML básico de la página (parte final)
    private static final String HTML_FIN = """
            </div>
        </body>
        </html>
        """;

    public static void main(String[] args) {
        // Iniciamos el servidor web en un hilo separado
        new Thread(() -> {
            try {
                iniciarServidorWeb();
            } catch (IOException e) {
                System.err.println("Error al iniciar el servidor: " + e.getMessage());
            }
        }).start();

        // Iniciamos la interfaz de consola
        iniciarInterfazConsola();
    }

    /**
     * Método para iniciar el servidor web que muestra los resultados
     */
    private static void iniciarServidorWeb() throws IOException {
        // Verificamos que exista la carpeta web
        File webDir = new File(CARPETA_WEB);
        if (!webDir.exists()) {
            System.err.println("ERROR: No se encuentra la carpeta 'web'");
            return;
        }

        try (// Creamos el servidor
        ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("\nServidor web iniciado en http://localhost:" + PUERTO);

            // Bucle principal del servidor
            while (true) {
                Socket socket = null;
                try {
                    // Esperamos una conexión
                    socket = serverSocket.accept();

                    // Leemos la solicitud del navegador
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String request = in.readLine();
                    if (request == null) continue;

                    // Preparamos la respuesta
                    OutputStream out = socket.getOutputStream();

                    // Procesamos la solicitud
                    String ruta = request.split(" ")[1];
                    
                    // Decodificar la URL para manejar caracteres especiales
                    try {
                        ruta = URLDecoder.decode(ruta, "UTF-8");
                    } catch (Exception e) {
                        // Si hay error en la decodificación, usamos la ruta original
                    }
                    
                    switch (ruta) {
                        case "/":
                            // Página principal
                            enviarRespuesta(out, "200 OK", "text/html", HTML_INICIO + HTML_FIN);
                            break;
                            
                        case "/evaluar":
                            // Resultados de un profesor (sin parámetros)
                            String htmlDefault = generarHtmlResultados(1);
                            enviarRespuesta(out, "200 OK", "text/html", HTML_INICIO + htmlDefault + HTML_FIN);
                            break;
                            
                        case "/style.css":
                            // Archivo CSS
                            servirArchivoEstatico(out, "/style.css");
                            break;
                            
                        default:
                            if (ruta.startsWith("/img/")) {
                                // Archivos de imagen
                                servirArchivoEstatico(out, ruta);
                            } else if (ruta.startsWith("/evaluar")) {
                                // Manejo de evaluación con parámetros
                                int profesorId = 1;
                                if (ruta.contains("profesor=")) {
                                    String[] partes = ruta.split("profesor=");
                                    if (partes.length > 1) {
                                        try {
                                            profesorId = Integer.parseInt(partes[1].trim());
                                        } catch (NumberFormatException e) {
                                            profesorId = 1;
                                        }
                                    }
                                }
                                String htmlResultados = generarHtmlResultados(profesorId);
                                enviarRespuesta(out, "200 OK", "text/html", HTML_INICIO + htmlResultados + HTML_FIN);
                            } else {
                                // Página no encontrada
                                enviarRespuesta(out, "404 Not Found", "text/html", "<h1>Página no encontrada</h1>");
                            }
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    if (socket != null) {
                        try { socket.close(); } catch (IOException e) {}
                    }
                }
            }
        }
    }

    /**
     * Método para iniciar la interfaz de consola para evaluar
     */
    private static void iniciarInterfazConsola() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== SISTEMA DE EVALUACIÓN DOCENTE ===");

        while (true) {
            System.out.println("\nMENÚ PRINCIPAL");
            System.out.println("1. Evaluar profesor");
            System.out.println("2. Ver resultados en navegador");
            System.out.println("3. Salir");
            System.out.print("Seleccione una opción: ");

            String opcion = scanner.nextLine();

            if (opcion.equals("3")) {
                break;
            } else if (opcion.equals("2")) {
                System.out.println("\nAbra su navegador en: http://localhost:" + PUERTO);
                continue;
            } else if (!opcion.equals("1")) {
                System.out.println("Opción no válida. Intente nuevamente.");
                continue;
            }

            // Selección de profesor
            System.out.println("\nPROFESORES DISPONIBLES:");
            System.out.println("1. Jaime Zapata - Metodologías Ágiles");
            System.out.println("2. Jaime Zapata - Lógica de Programación");
            System.out.println("3. Luis Zapata - Introducción a la programación");
            System.out.print("Seleccione un profesor (1-3): ");

            int profesorId;
            try {
                profesorId = Integer.parseInt(scanner.nextLine());
                if (profesorId < 1 || profesorId > 3) {
                    System.out.println("Número de profesor no válido.");
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("Debe ingresar un número.");
                continue;
            }

            // Evaluamos cada categoría
            int[] metodologia = evaluarMetodologia(scanner);
            int[] comunicacion = evaluarComunicacion(scanner);
            int[] evaluacion = evaluarEvaluacion(scanner);
            int[] actitudes = evaluarActitudes(scanner);
            int[] valoracion = evaluarValoracionGeneral(scanner);

            // Almacenamos las respuestas
            evaluaciones.put(profesorId, new int[][]{metodologia, comunicacion, evaluacion, actitudes, valoracion});

            System.out.println("\n¡Evaluación completada con éxito!");
        }

        scanner.close();
    }

    // Métodos para evaluar cada categoría (similares para todas las categorías)
    private static int[] evaluarMetodologia(Scanner scanner) {
        System.out.println("\n=== METODOLOGÍA Y ESTRATEGIAS DE ENSEÑANZA ===");
        int[] respuestas = new int[5];

        System.out.println("\n1. El docente presenta los contenidos de manera clara y estructurada.");
        respuestas[0] = obtenerRespuesta(scanner);

        System.out.println("\n2. Utiliza recursos didácticos adecuados (presentaciones, videos, prácticas, etc.).");
        respuestas[1] = obtenerRespuesta(scanner);

        System.out.println("\n3. Promueve la participación activa de los estudiantes durante la clase.");
        respuestas[2] = obtenerRespuesta(scanner);

        System.out.println("\n4. Relaciona los contenidos con aplicaciones reales o del entorno técnico.");
        respuestas[3] = obtenerRespuesta(scanner);

        System.out.println("\n5. Estimula el pensamiento crítico y la resolución de problemas.");
        respuestas[4] = obtenerRespuesta(scanner);

        // Solicitamos comentario pero no lo guardamos (según requisito)
        System.out.print("\nComentario opcional (no se guardará): ");
        scanner.nextLine();

        return respuestas;
    }

    private static int[] evaluarComunicacion(Scanner scanner) {
        System.out.println("\n=== COMUNICACIÓN Y RELACIÓN CON EL ESTUDIANTE ===");
        int[] respuestas = new int[3];

        System.out.println("\n1. Se comunica de manera respetuosa y profesional.");
        respuestas[0] = obtenerRespuesta(scanner);

        System.out.println("\n2. Escucha y responde adecuadamente a las preguntas de los estudiantes.");
        respuestas[1] = obtenerRespuesta(scanner);

        System.out.println("\n3. Está disponible para atender consultas fuera del horario de clase.");
        respuestas[2] = obtenerRespuesta(scanner);

        // Solicitamos comentario pero no lo guardamos
        System.out.print("\nComentario opcional (no se guardará): ");
        scanner.nextLine();

        return respuestas;
    }

    private static int[] evaluarEvaluacion(Scanner scanner) {
        System.out.println("\n=== EVALUACIÓN Y RETROALIMENTACIÓN ===");
        int[] respuestas = new int[3];

        System.out.println("\n1. Informa claramente los criterios de evaluación.");
        respuestas[0] = obtenerRespuesta(scanner);

        System.out.println("\n2. Las evaluaciones están relacionadas con los contenidos enseñados.");
        respuestas[1] = obtenerRespuesta(scanner);

        System.out.println("\n3. Entrega retroalimentación oportuna sobre el desempeño académico.");
        respuestas[2] = obtenerRespuesta(scanner);

        return respuestas;
    }

    private static int[] evaluarActitudes(Scanner scanner) {
        System.out.println("\n=== ACTITUDES Y PROFESIONALISMO ===");
        int[] respuestas = new int[3];

        System.out.println("\n1. Muestra compromiso con el proceso educativo.");
        respuestas[0] = obtenerRespuesta(scanner);

        System.out.println("\n2. Demuestra dominio del tema o materia que imparte.");
        respuestas[1] = obtenerRespuesta(scanner);

        System.out.println("\n3. Es puntual y cumple con los horarios establecidos.");
        respuestas[2] = obtenerRespuesta(scanner);

        return respuestas;
    }

    private static int[] evaluarValoracionGeneral(Scanner scanner) {
        System.out.println("\n=== VALORACIÓN GENERAL ===");
        int[] respuestas = new int[1];

        System.out.println("\n1. En general, estoy satisfecho/a con el desempeño del docente.");
        respuestas[0] = obtenerRespuesta(scanner);

        // Solicitamos comentario pero no lo guardamos
        System.out.print("\nComentario opcional (no se guardará): ");
        scanner.nextLine();

        return respuestas;
    }

    /**
     * Método auxiliar para obtener una respuesta válida (1-5)
     */
    private static int obtenerRespuesta(Scanner scanner) {
        while (true) {
            System.out.print("Ingrese puntuación (1-5): ");
            try {
                int respuesta = Integer.parseInt(scanner.nextLine());
                if (respuesta >= 1 && respuesta <= 5) {
                    return respuesta;
                }
                System.out.println("Error: Debe ser entre 1 y 5");
            } catch (NumberFormatException e) {
                System.out.println("Error: Ingrese un número válido");
            }
        }
    }

    /**
     * Método para enviar una respuesta HTTP
     */
    private static void enviarRespuesta(OutputStream out, String estado, String tipo, String contenido) throws IOException {
        String respuesta = "HTTP/1.1 " + estado + "\r\n" +
                "Content-Type: " + tipo + "\r\n" +
                "Content-Length: " + contenido.length() + "\r\n" +
                "\r\n" + contenido;
        out.write(respuesta.getBytes());
    }

    /**
     * Método para servir archivos estáticos (CSS, imágenes)
     */
    private static void servirArchivoEstatico(OutputStream out, String rutaArchivo) throws IOException {
        File archivo = new File(CARPETA_WEB + rutaArchivo);
        if (!archivo.exists()) {
            enviarRespuesta(out, "404 Not Found", "text/html", "<h1>Archivo no encontrado</h1>");
            return;
        }

        // Determinamos el tipo de contenido
        String tipo = "application/octet-stream";
        if (rutaArchivo.endsWith(".css")) tipo = "text/css";
        else if (rutaArchivo.endsWith(".svg")) tipo = "image/svg+xml";
        else if (rutaArchivo.endsWith(".jfif") || rutaArchivo.endsWith(".jpg")) tipo = "image/jpeg";
        else if (rutaArchivo.endsWith(".png")) tipo = "image/png";

        // Enviamos el archivo
        String cabecera = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + tipo + "\r\n" +
                "Content-Length: " + archivo.length() + "\r\n" +
                "\r\n";
        out.write(cabecera.getBytes());
        Files.copy(archivo.toPath(), out);
    }

    /**
     * Método para generar el HTML con los resultados de un profesor
     */
    private static String generarHtmlResultados(int profesorId) {
        // Verificamos si hay evaluaciones para este profesor
        if (!evaluaciones.containsKey(profesorId)) {
            return "<div class='parte-4'><h2>No hay evaluaciones registradas para este profesor</h2></div>";
        }

        int[][] resultados = evaluaciones.get(profesorId);

        // Datos de los profesores
        String[] nombres = {"Jaime Zapata", "Jaime Zapata", "Luis Zapata"};
        String[] cursos = {"Metodologías Ágiles", "Lógica de Programación", "Introducción a la programación"};
        String[] imagenes = {"/img/profesor.jpg", "/img/profesor.jpg", "/img/profesor.jpg"};

        // Generamos el HTML
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"parte-4\">");

        // Tarjeta para Metodología
        html.append(crearTarjetaResultados(
                "Metodología y Estrategias de Enseñanza",
                nombres[profesorId-1],
                cursos[profesorId-1],
                imagenes[profesorId-1],
                resultados[0],
                new String[]{
                        "Claridad en contenidos",
                        "Recursos didácticos",
                        "Participación activa",
                        "Aplicaciones reales",
                        "Pensamiento crítico"
                }
        ));

        // Tarjeta para Comunicación
        html.append(crearTarjetaResultados(
                "Comunicación y Relación con el Estudiante",
                nombres[profesorId-1],
                cursos[profesorId-1],
                imagenes[profesorId-1],
                resultados[1],
                new String[]{
                        "Comunicación respetuosa",
                        "Respuesta a preguntas",
                        "Disponibilidad"
                }
        ));

        // Tarjeta para Evaluación
        html.append(crearTarjetaResultados(
                "Evaluación y Retroalimentación",
                nombres[profesorId-1],
                cursos[profesorId-1],
                imagenes[profesorId-1],
                resultados[2],
                new String[]{
                        "Criterios de evaluación",
                        "Relación con contenidos",
                        "Retroalimentación oportuna"
                }
        ));

        // Tarjeta para Actitudes
        html.append(crearTarjetaResultados(
                "Actitudes y Profesionalismo",
                nombres[profesorId-1],
                cursos[profesorId-1],
                imagenes[profesorId-1],
                resultados[3],
                new String[]{
                        "Compromiso educativo",
                        "Dominio del tema",
                        "Puntualidad"
                }
        ));

        // Tarjeta para Valoración General
        html.append(crearTarjetaResultados(
                "Valoración General",
                nombres[profesorId-1],
                cursos[profesorId-1],
                imagenes[profesorId-1],
                resultados[4],
                new String[]{"Satisfacción general"}
        ));

        html.append("</div>");
        return html.toString();
    }

    /**
     * Método auxiliar para crear una tarjeta de resultados
     */
    private static String crearTarjetaResultados(String titulo, String profesor, String curso,
                                                 String imagen, int[] datos, String[] etiquetas) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"card-h\">")
                .append("<div class=\"card-title\"><h2>").append(titulo).append("</h2></div>")
                .append("<div class=\"card-content\">")
                .append("<div class=\"card-img\"><img src=\"").append(imagen).append("\" alt=\"\"></div>")
                .append("<div class=\"card-cargo\"><h3>").append(profesor).append("</h3><p>").append(curso).append("</p></div>")
                .append("<div class=\"card-graphic\">")
                .append("<table class=\"charts-css column show-primary-axis show-4-secondary-axes show-data-axes data-spacing-15\">")
                .append("<caption>Resultados de Evaluación</caption><tbody>");

        for (int i = 0; i < datos.length; i++) {
            html.append("<tr><th scope=\"row\" title=\"").append(etiquetas[i]).append("\">")
                    .append(i+1).append("</th>")
                    .append("<td style=\"--size: calc(").append(datos[i]).append(" / 5); --color: #EE2B7B\">")
                    .append("<span class=\"data-label\">").append(datos[i]).append("</span></td></tr>");
        }

        html.append("</tbody></table></div></div></div>");
        return html.toString();
    }
}