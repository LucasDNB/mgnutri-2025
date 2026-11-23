package ar.edu.usal.mgnutri.configuracion.service;

import ar.edu.usal.mgnutri.catalogo.dao.EjercicioDao;
import ar.edu.usal.mgnutri.catalogo.domain.Ejercicio;
import ar.edu.usal.mgnutri.catalogo.service.AlimentoService;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.entrenamiento.dao.RegistroEntrenamientoDao;
import ar.edu.usal.mgnutri.entrenamiento.domain.RegistroEntrenamiento;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaDto;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.EjercicioItemDto;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaRequest;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaResponse;
import ar.edu.usal.mgnutri.entrenamiento.service.RutinaService;
import ar.edu.usal.mgnutri.evolucion.dao.MedicionAntropometricaDao;
import ar.edu.usal.mgnutri.evolucion.domain.MedicionAntropometrica;
import ar.edu.usal.mgnutri.evolucion.service.EvolucionService;
import ar.edu.usal.mgnutri.nutricion.domain.TipoComida;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.BorradorGenerado;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaDto;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaParam;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.GenerarRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ItemDto;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanRequest;
import ar.edu.usal.mgnutri.nutricion.service.GeneradorPlanService;
import ar.edu.usal.mgnutri.nutricion.service.PlanAlimentarioService;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AltaResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AsignacionItem;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PacienteRequest;
import ar.edu.usal.mgnutri.pacientes.service.PacienteService;
import ar.edu.usal.mgnutri.pagos.domain.Pago;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.ArancelRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.CargoRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.PagoRequest;
import ar.edu.usal.mgnutri.pagos.service.ArancelService;
import ar.edu.usal.mgnutri.pagos.service.CargoService;
import ar.edu.usal.mgnutri.pagos.service.PagoService;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.FranjaDto;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalRequest;
import ar.edu.usal.mgnutri.profesionales.service.AgendaService;
import ar.edu.usal.mgnutri.profesionales.service.ProfesionalService;
import ar.edu.usal.mgnutri.seguridad.dao.UsuarioDao;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import ar.edu.usal.mgnutri.turnos.dao.TurnoDao;
import ar.edu.usal.mgnutri.turnos.domain.Turno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Datos de demostración (spec 16, perfil "demo"): 2 nutricionistas, 2 entrenadores, 1 integral y 30 pacientes con
 * turnos, planes, rutinas, 6 meses de evolución, pagos y deudas. Se ejecuta una sola vez (si no hay profesionales).
 * Todas las cuentas demo usan la contraseña DEMO_PASSWORD (por defecto "Demo2025!") y ya aceptaron los T&C.
 * Los datos imitan la realidad (DNI acorde a la edad, nombres, localidades y objetivos variados), pero los mails usan el
 * dominio reservado example.com y los teléfonos el prefijo 11 00 (no asignado): si se activa WhatsApp no se escribe a terceros.
 */
@Component
@Profile("demo")
@Order(10)
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final String[] APELLIDOS = {"Gómez", "Fernández", "Rodríguez", "López", "Martínez", "Pérez", "García", "Sánchez", "Romero",
            "Sosa", "Torres", "Álvarez", "Ruiz", "Ramírez", "Flores", "Acosta", "Benítez", "Medina", "Herrera", "Suárez", "Aguirre", "Giménez",
            "Molina", "Silva", "Castro", "Rojas", "Ortiz", "Núñez", "Luna", "Juárez", "Ferreyra", "Quiroga", "Villalba", "Cabrera",
            "Godoy", "Ledesma", "Vega", "Coronel", "Ojeda", "Bustos", "Figueroa", "Mansilla", "Peralta", "Toledo", "Lucero", "Navarro"};
    private static final String[] NOMBRES_F = {"Ana", "Carla", "Elena", "Gabriela", "Inés", "Karina", "Micaela", "Olivia", "Rocío", "Tamara",
            "Valentina", "Ximena", "Yamila", "Zoe", "Belén", "Daniela", "Florencia", "Agustina", "Camila", "Julieta", "Romina", "Lucía",
            "Milagros", "Sabrina", "Natalia", "Verónica", "Silvina", "Lorena", "Antonella", "Victoria"};
    private static final String[] NOMBRES_M = {"Bruno", "Diego", "Federico", "Hernán", "Julián", "Lucas", "Nicolás", "Pablo", "Santiago", "Ulises",
            "Walter", "Agustín", "Camilo", "Matías", "Facundo", "Gonzalo", "Joaquín", "Emiliano", "Leandro", "Franco", "Ezequiel", "Mariano",
            "Sebastián", "Gustavo", "Javier", "Alejandro", "Maximiliano", "Ramiro", "Tobías", "Ignacio"};
    private static final String[] LOCALIDADES = {"Pilar", "Del Viso", "Manuel Alberti", "Villa Rosa", "Derqui", "Tortuguitas", "Garín",
            "Escobar", "Maquinista Savio", "José C. Paz", "San Miguel", "Benavídez", "Tigre", "San Isidro", "Martínez"};
    private static final String[] OBJETIVOS = {"Descenso de peso", "Ganancia de masa muscular", "Mejorar hábitos y rendimiento",
            "Control de glucemia (indicación médica)", "Preparación para media maratón", "Recomposición corporal",
            "Alimentación vegetariana equilibrada", "Recuperación post lesión de rodilla", "Mejorar energía y descanso",
            "Preparación física para fútbol amateur"};
    private static final String[] CALLES = {"Av. Tomás Márquez", "Chacabuco", "Rivadavia", "Belgrano", "Las Heras", "Ruta 8 km 50",
            "Av. Caamaño", "Independencia", "Hipólito Yrigoyen", "Lisandro de la Torre", "Champagnat", "Pedro Lagrave"};

    private final ProfesionalDao profesionalDao;
    private final ProfesionalService profesionales;
    private final AgendaService agenda;
    private final PacienteService pacientes;
    private final PacienteDao pacienteDao;
    private final UsuarioDao usuarioDao;
    private final PasswordEncoder encoder;
    private final GeneradorPlanService generador;
    private final AlimentoService alimentos;
    private final PlanAlimentarioService planes;
    private final RutinaService rutinas;
    private final EjercicioDao ejercicioDao;
    private final RegistroEntrenamientoDao registroDao;
    private final MedicionAntropometricaDao medicionDao;
    private final TurnoDao turnoDao;
    private final ArancelService aranceles;
    private final CargoService cargos;
    private final PagoService pagos;
    private final ConfiguracionService config;
    private final Clock clock;
    private final String password = System.getenv().getOrDefault("DEMO_PASSWORD", "Demo2025!");
    /** Tamaño del dataset: 30 pacientes / 6 meses por defecto; la prueba de rendimiento (spec 16) usa 500 / 12. */
    private final int cantidadPacientes = Integer.parseInt(System.getenv().getOrDefault("DEMO_PACIENTES", "30"));
    private final int meses = Integer.parseInt(System.getenv().getOrDefault("DEMO_MESES", "6"));

    public DemoDataLoader(ProfesionalDao profesionalDao, ProfesionalService profesionales, AgendaService agenda, PacienteService pacientes,
                          PacienteDao pacienteDao, UsuarioDao usuarioDao, PasswordEncoder encoder, GeneradorPlanService generador,
                          AlimentoService alimentos, PlanAlimentarioService planes, RutinaService rutinas, EjercicioDao ejercicioDao,
                          RegistroEntrenamientoDao registroDao, MedicionAntropometricaDao medicionDao, TurnoDao turnoDao,
                          ArancelService aranceles, CargoService cargos, PagoService pagos, ConfiguracionService config, Clock clock) {
        this.profesionalDao = profesionalDao;
        this.profesionales = profesionales;
        this.agenda = agenda;
        this.pacientes = pacientes;
        this.pacienteDao = pacienteDao;
        this.usuarioDao = usuarioDao;
        this.encoder = encoder;
        this.generador = generador;
        this.alimentos = alimentos;
        this.planes = planes;
        this.rutinas = rutinas;
        this.ejercicioDao = ejercicioDao;
        this.registroDao = registroDao;
        this.medicionDao = medicionDao;
        this.turnoDao = turnoDao;
        this.aranceles = aranceles;
        this.cargos = cargos;
        this.pagos = pagos;
        this.config = config;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (profesionalDao.count() > 0) {
            log.info("Datos demo: ya existen profesionales, no se cargan.");
            return;
        }
        log.info("Cargando datos de demostración…");
        Random rnd = new Random(2025);
        Random datos = new Random(1810); // datos personales: separado de rnd para no alterar la historia generada
        try {
            Usuario admin = usuarioDao.findByUsername("admin").orElseThrow();
            comoUsuario(admin.getId(), "admin", EnumSet.of(Rol.ADMIN), null, null);
            List<Profesional> profs = new ArrayList<>();
            profs.add(profesional("Laura", "López", "28456712", Profesional.Tipo.NUTRICIONISTA, "MN 7412", "Nutrición deportiva"));
            profs.add(profesional("Sofía", "Méndez", "31209845", Profesional.Tipo.NUTRICIONISTA, "MN 9035", "Nutrición clínica"));
            profs.add(profesional("Martín", "García", "30117652", Profesional.Tipo.ENTRENADOR, null, "Hipertrofia y fuerza"));
            profs.add(profesional("Tomás", "Ibarra", "33874019", Profesional.Tipo.ENTRENADOR, null, "Funcional y running"));
            profs.add(profesional("Mariana", "Gutiérrez", "29563388", Profesional.Tipo.INTEGRAL, "MN 8127", "Nutrición y entrenamiento"));
            for (Profesional p : profs) {
                if (!p.getTipo().usaAgenda()) {
                    continue; // los turnos son sólo de nutrición (v1.1)
                }
                List<FranjaDto> franjas = new ArrayList<>();
                for (int d = 1; d <= 5; d++) {
                    franjas.add(new FranjaDto(d, LocalTime.of(9, 0), LocalTime.of(13, 0), "Consultorio " + (profs.indexOf(p) + 1)));
                    franjas.add(new FranjaDto(d, LocalTime.of(15, 0), LocalTime.of(19, 0), "Consultorio " + (profs.indexOf(p) + 1)));
                }
                franjas.add(new FranjaDto(6, LocalTime.of(9, 0), LocalTime.of(12, 0), "Sede central"));
                agenda.reemplazarFranjas(p.getId(), franjas);
            }
            var arancel = aranceles.crear(new ArancelRequest("Cuota mensual", new BigDecimal("25000"), LocalDate.now(clock).minusMonths(meses), true, false, null));
            List<Long> pacienteIds = new ArrayList<>();
            for (int i = 0; i < cantidadPacientes; i++) {
                Profesional nut = profs.get(i % 3 == 2 ? 4 : i % 2);
                Profesional ent = profs.get(i % 3 == 2 ? 4 : 2 + i % 2);
                boolean mujer = i % 2 == 0;
                String nombre = mujer ? NOMBRES_F[(i * 7 + i / NOMBRES_F.length) % NOMBRES_F.length] : NOMBRES_M[(i * 11 + i / NOMBRES_M.length) % NOMBRES_M.length];
                String apellido = APELLIDOS[(i * 13 + i / APELLIDOS.length * 5) % APELLIDOS.length];
                LocalDate nacimiento = LocalDate.of(1972 + datos.nextInt(34), 1 + datos.nextInt(12), 1 + datos.nextInt(28));
                // DNI aproximado según el año de nacimiento (1972 ≈ 22 M, 2005 ≈ 46 M), único por el sufijo i
                String dni = String.valueOf(22_000_000 + (nacimiento.getYear() - 1972) * 720_000 + datos.nextInt(600) * 1000 + i);
                String telefono = "1100" + String.format("%06d", 100_000 + i * 7919 % 900_000);
                String email = (sinAcentos(nombre) + "." + sinAcentos(apellido)).toLowerCase().replace(' ', '.') + i + "@example.com";
                String domicilio = CALLES[datos.nextInt(CALLES.length)] + " " + (100 + datos.nextInt(3900)) + ", " + LOCALIDADES[datos.nextInt(LOCALIDADES.length)];
                AltaResponse a = pacientes.crear(new PacienteRequest(nombre, apellido, "DNI", dni, nacimiento,
                        mujer ? "F" : "M", telefono, email, domicilio, OBJETIVOS[datos.nextInt(OBJETIVOS.length)], null, null,
                        List.of(new AsignacionItem(nut.getId(), Area.NUTRICION), new AsignacionItem(ent.getId(), Area.ENTRENAMIENTO)), null));
                activarCuenta(a.username());
                pacienteIds.add(a.id());
                cuentaCorriente(a.id(), arancel.id(), rnd, i);
                historialTurnos(a.id(), nut, ent, rnd);
                mediciones(a.id(), nut, rnd, i);
            }
            for (Profesional p : profs) {
                activarCuenta(p.getNroDocumento());
            }
            // planes y rutinas creados por cada profesional
            for (int i = 0; i < pacienteIds.size(); i++) {
                Long pid = pacienteIds.get(i);
                Profesional nut = profs.get(i % 3 == 2 ? 4 : i % 2);
                Profesional ent = profs.get(i % 3 == 2 ? 4 : 2 + i % 2);
                comoProfesional(nut);
                plan(pid, i);
                comoProfesional(ent);
                RutinaResponse r = rutina(pid, i, rnd);
                registrosEntrenamiento(pid, r, rnd);
            }
            log.info("Datos demo cargados: 5 profesionales y " + cantidadPacientes + " pacientes (" + meses + " meses de historia). Contraseña de las cuentas demo: la definida en DEMO_PASSWORD.");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String sinAcentos(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private Profesional profesional(String nombre, String apellido, String doc, Profesional.Tipo tipo, String matricula, String esp) {
        var r = profesionales.crear(new ProfesionalRequest(nombre, apellido, "DNI", doc, tipo, matricula, esp, "1100" + doc.substring(2, 8),
                sinAcentos(nombre + "." + apellido).toLowerCase() + "@example.com", tipo == Profesional.Tipo.ENTRENADOR ? 45 : 30, null));
        return profesionalDao.findById(r.id()).orElseThrow();
    }

    private void activarCuenta(String username) {
        Usuario u = usuarioDao.findByUsername(username).orElseThrow();
        u.setPasswordHash(encoder.encode(password));
        u.setDebeCambiarPassword(false);
        u.setTerminosVersionAceptada(config.versionTerminosVigente());
        u.setTerminosAceptadosAt(clock.instant().truncatedTo(ChronoUnit.SECONDS));
        usuarioDao.save(u);
    }

    private void cuentaCorriente(Long pid, Long arancelId, Random rnd, int i) {
        LocalDate hoy = LocalDate.now(clock);
        for (int m = meses - 1; m >= 0; m--) {
            LocalDate f = hoy.minusMonths(m).withDayOfMonth(1);
            String periodo = f.toString().substring(0, 7);
            cargos.registrar(pid, new CargoRequest("Cuota mensual", arancelId, periodo, new BigDecimal("25000"), f, f.plusDays(10), false, null, null));
            boolean paga = m > (i % 4 == 0 ? 2 : 0) || rnd.nextInt(10) < 3;
            if (paga) {
                Pago.Medio medio = Pago.Medio.values()[rnd.nextInt(5)];
                pagos.registrar(pid, new PagoRequest(new BigDecimal("25000"), f.plusDays(rnd.nextInt(9)), medio, null, null, null, null));
            }
        }
    }

    private void historialTurnos(Long pid, Profesional nut, Profesional ent, Random rnd) {
        LocalDate hoy = LocalDate.now(clock);
        for (int s = meses * 4; s >= 1; s -= 3) {
            for (Profesional p : List.of(nut)) { // los turnos son sólo de nutrición (v1.1)
                LocalDate dia = hoy.minusWeeks(s).with(java.time.DayOfWeek.MONDAY).plusDays(rnd.nextInt(5));
                Instant inicio = Fechas.instante(dia, LocalTime.of(9 + rnd.nextInt(4), rnd.nextBoolean() ? 0 : 30));
                Turno t = new Turno();
                t.setPacienteId(pid);
                t.setProfesionalId(p.getId());
                t.setArea(Area.NUTRICION);
                t.setInicio(inicio);
                t.setFin(inicio.plus(Duration.ofMinutes(p.getDuracionTurnoMin())));
                t.setUbicacion("Consultorio");
                int r = rnd.nextInt(10);
                t.setEstado(r < 7 ? Turno.Estado.ATENDIDO : r < 9 ? Turno.Estado.AUSENTE : Turno.Estado.CANCELADO);
                if (t.getEstado() == Turno.Estado.CANCELADO) {
                    t.setMotivoCancelacion("Cancelado por el paciente");
                    t.setCanceladoPorRol("PACIENTE");
                }
                try {
                    turnoDao.saveAndFlush(t);
                } catch (RuntimeException e) {
                    // colisión improbable de horario: se omite
                }
            }
        }
        // próximo turno reservado
        LocalDate prox = hoy.plusDays(3 + rnd.nextInt(10));
        if (prox.getDayOfWeek().getValue() > 5) {
            prox = prox.plusDays(8 - prox.getDayOfWeek().getValue());
        }
        Instant inicio = Fechas.instante(prox, LocalTime.of(15 + rnd.nextInt(3), rnd.nextBoolean() ? 0 : 30));
        Turno t = new Turno();
        t.setPacienteId(pid);
        t.setProfesionalId(nut.getId());
        t.setArea(Area.NUTRICION);
        t.setInicio(inicio);
        t.setFin(inicio.plus(Duration.ofMinutes(nut.getDuracionTurnoMin())));
        t.setUbicacion("Consultorio");
        try {
            turnoDao.saveAndFlush(t);
        } catch (RuntimeException e) {
            // se omite
        }
    }

    private void mediciones(Long pid, Profesional nut, Random rnd, int i) {
        double peso = 60 + rnd.nextInt(35);
        double talla = 155 + rnd.nextInt(30);
        double grasa = 20 + rnd.nextInt(12);
        double musculo = 30 + rnd.nextInt(8);
        double cintura = 70 + rnd.nextInt(25);
        boolean baja = i % 3 == 0;
        for (int m = meses; m >= 0; m--) {
            MedicionAntropometrica x = new MedicionAntropometrica();
            x.setPacienteId(pid);
            x.setProfesionalId(nut.getId());
            x.setFecha(LocalDate.now(clock).minusMonths(m).minusDays(rnd.nextInt(5)));
            x.setPesoKg(bd(peso));
            x.setTallaCm(bd(talla));
            x.setImc(EvolucionService.imc(bd(peso), bd(talla)));
            x.setPctGrasa(bd(grasa));
            x.setPctMusculo(bd(musculo));
            x.setCircCintura(bd(cintura));
            x.setCircCadera(bd(cintura + 18));
            x.setCircBrazo(bd(28 + rnd.nextInt(5)));
            medicionDao.save(x);
            peso += baja ? -(0.4 + rnd.nextDouble()) : 0.2 + rnd.nextDouble() * 0.6;
            grasa += baja ? -0.6 : -0.3;
            musculo += baja ? 0.1 : 0.5;
            cintura += baja ? -0.8 : -0.2;
        }
    }

    private void plan(Long pid, int i) {
        List<ComidaParam> comidas = List.of(new ComidaParam(TipoComida.DESAYUNO, 20.0), new ComidaParam(TipoComida.COLACION_MANANA, 10.0),
                new ComidaParam(TipoComida.ALMUERZO, 30.0), new ComidaParam(TipoComida.MERIENDA, 15.0), new ComidaParam(TipoComida.CENA, 25.0));
        int kcal = i % 3 == 0 ? 1700 : i % 3 == 1 ? 2600 : 2200;
        BorradorGenerado b = generador.generar(new GenerarRequest(pid, kcal, 50.0, 20.0, 30.0, comidas, List.of(), i % 2 == 0 ? "DIARIA" : "SEMANAL", (long) i),
                alimentos.activos());
        List<ComidaDto> cs = b.comidas().stream().map(c -> new ComidaDto(c.diaSemana(), c.tipo(), c.descripcion(),
                c.items().stream().map(it -> new ItemDto(null, it.alimentoId(), it.gramos(), it.cantidadCasera())).toList())).toList();
        planes.crear(pid, new PlanRequest("Plan " + LocalDate.now(clock).getMonth().getDisplayName(java.time.format.TextStyle.FULL, Fechas.ES_AR),
                LocalDate.now(clock).minusDays(14), 8, "Hidratarse con 2 litros de agua por día.", "AUTOMATICO", kcal,
                BigDecimal.valueOf(50), BigDecimal.valueOf(20), BigDecimal.valueOf(30), null, cs, "ACTIVO", true, null));
    }

    private RutinaResponse rutina(Long pid, int i, Random rnd) {
        List<Ejercicio> todos = ejercicioDao.findAll().stream().filter(Ejercicio::isActivo).toList();
        List<DiaDto> dias = new ArrayList<>();
        int[] diasSemana = i % 2 == 0 ? new int[]{1, 3, 5} : new int[]{2, 4, 6};
        String[] nombres = {"Tren superior", "Tren inferior", "Full body"};
        for (int k = 0; k < diasSemana.length; k++) {
            List<EjercicioItemDto> ejs = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                Ejercicio e = todos.get(rnd.nextInt(todos.size()));
                ejs.add(new EjercicioItemDto(null, e.getId(), 3 + rnd.nextInt(2), j % 2 == 0 ? "10" : "8-12", 60 + 30 * rnd.nextInt(3), null, null, null, null));
            }
            dias.add(new DiaDto(diasSemana[k], nombres[k], ejs));
        }
        return rutinas.crear(pid, new RutinaRequest(i % 2 == 0 ? "Fuerza fase 1" : "Acondicionamiento general", "Mejorar fuerza y composición corporal",
                LocalDate.now(clock).minusDays(21), 8, "Entrada en calor de 10 minutos antes de cada sesión.", dias, true, null));
    }

    private void registrosEntrenamiento(Long pid, RutinaResponse r, Random rnd) {
        LocalDate hoy = LocalDate.now(clock);
        int semana = 0;
        for (LocalDate f = hoy.minusDays(20); f.isBefore(hoy); f = f.plusDays(1)) {
            int dia = f.getDayOfWeek().getValue();
            if (dia == 1) {
                semana++;
            }
            for (DiaResponse d : r.dias()) {
                if (d.diaSemana() != dia || rnd.nextInt(10) < 2) {
                    continue;
                }
                int k = 0;
                for (var e : d.ejercicios()) {
                    RegistroEntrenamiento reg = new RegistroEntrenamiento(pid, e.id(), e.ejercicioId(), f, clock.instant().truncatedTo(ChronoUnit.SECONDS));
                    reg.setCompletado(rnd.nextInt(10) < 8);
                    reg.setCargaKg(BigDecimal.valueOf(15 + 5L * k + 2.5 * semana));
                    reg.setSeriesRealizadas(e.series());
                    reg.setRepeticionesRealizadas(10);
                    registroDao.save(reg);
                    k++;
                }
            }
        }
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_EVEN);
    }

    private void comoProfesional(Profesional p) {
        Usuario u = usuarioDao.findById(p.getUsuarioId()).orElseThrow();
        comoUsuario(u.getId(), u.getUsername(), EnumSet.copyOf(p.getTipo().roles()), null, p.getId());
    }

    private void comoUsuario(Long id, String username, EnumSet<Rol> roles, Long pacienteId, Long profesionalId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(id, username, roles, pacienteId, profesionalId, false, false, "demo");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList()));
    }
}
