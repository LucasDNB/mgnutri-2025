package ar.edu.usal.mgnutri;

import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Spec 16: reglas de arquitectura (capas, auditoría, entidades no expuestas). */
class ArquitecturaTest {

    static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("ar.edu.usal.mgnutri");

    @Test
    void controllersNoAccedenADaos() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..dao..")
                .check(CLASES);
    }

    @Test
    void daosNoDependenDeServicios() {
        noClasses().that().resideInAPackage("..dao..")
                .should().dependOnClassesThat().resideInAPackage("..service..")
                .check(CLASES);
    }

    @Test
    void controllersNoExponenEntidades() {
        methods().that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class).and().arePublic()
                .should().haveRawReturnType(com.tngtech.archunit.base.DescribedPredicate.describe("no es entidad JPA",
                        (com.tngtech.archunit.core.domain.JavaClass c) -> !c.isAnnotatedWith(Entity.class)))
                .check(CLASES);
    }

    @Test
    void serviciosDeEscrituraEstanAuditados() {
        methods().that().areDeclaredInClassesThat().resideInAPackage("..service..")
                .and().arePublic()
                .and().haveNameMatching("crear|modificar|darDeBaja|anular|eliminar|registrar(Pago)?")
                .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Service")
                .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("Bitacora")
                .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("RegistroAcceso")
                .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("Usuario")
                .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("ActividadFisica")
                .and().areDeclaredInClassesThat().haveSimpleNameNotContaining("ActividadNutricional")
                .should().beAnnotatedWith(Auditable.class)
                .allowEmptyShould(true)
                .check(CLASES);
    }
}
