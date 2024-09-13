package com.application.views.servicios;

import com.application.data.Servicio;
import com.application.services.ServicioService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Servicios")
@Menu(icon = "line-awesome/svg/ambulance-solid.svg", order = 3)
@Route(value = "master-detail/:servicioID?/:action?(edit)")
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class ServiciosView extends Div implements BeforeEnterObserver {

    private final String SERVICIO_ID = "servicioID";
    private final String SERVICIO_EDIT_ROUTE_TEMPLATE = "master-detail/%s/edit";

    private final Grid<Servicio> grid = new Grid<>(Servicio.class, false);

    private Upload foto;
    private Image fotoPreview;
    private TextField nombre;
    private Checkbox activo;
    private TextField costo;
    private TextField descripcion;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<Servicio> binder;

    private Servicio servicio;

    private final ServicioService servicioService;

    public ServiciosView(ServicioService servicioService) {
        this.servicioService = servicioService;
        addClassNames("servicios-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        LitRenderer<Servicio> fotoRenderer = LitRenderer.<Servicio>of("<img style='height: 64px' src=${item.foto} />")
                .withProperty("foto", item -> {
                    if (item != null && item.getFoto() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getFoto());
                    } else {
                        return "";
                    }
                });
        grid.addColumn(fotoRenderer).setHeader("Foto").setWidth("68px").setFlexGrow(0);

        grid.addColumn("nombre").setAutoWidth(true);
        LitRenderer<Servicio> activoRenderer = LitRenderer.<Servicio>of(
                "<vaadin-icon icon='vaadin:${item.icon}' style='width: var(--lumo-icon-size-s); height: var(--lumo-icon-size-s); color: ${item.color};'></vaadin-icon>")
                .withProperty("icon", activo -> activo.isActivo() ? "check" : "minus").withProperty("color",
                        activo -> activo.isActivo()
                                ? "var(--lumo-primary-text-color)"
                                : "var(--lumo-disabled-text-color)");

        grid.addColumn(activoRenderer).setHeader("Activo").setAutoWidth(true);

        grid.addColumn("costo").setAutoWidth(true);
        grid.addColumn("descripcion").setAutoWidth(true);
        grid.setItems(query -> servicioService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(SERVICIO_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ServiciosView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(Servicio.class);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.forField(costo).withConverter(new StringToIntegerConverter("Only numbers are allowed")).bind("costo");

        binder.bindInstanceFields(this);

        attachImageUpload(foto, fotoPreview);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.servicio == null) {
                    this.servicio = new Servicio();
                }
                binder.writeBean(this.servicio);
                servicioService.update(this.servicio);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(ServiciosView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> servicioId = event.getRouteParameters().get(SERVICIO_ID).map(Long::parseLong);
        if (servicioId.isPresent()) {
            Optional<Servicio> servicioFromBackend = servicioService.get(servicioId.get());
            if (servicioFromBackend.isPresent()) {
                populateForm(servicioFromBackend.get());
            } else {
                Notification.show(String.format("The requested servicio was not found, ID = %s", servicioId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(ServiciosView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        NativeLabel fotoLabel = new NativeLabel("Foto");
        fotoPreview = new Image();
        fotoPreview.setWidth("100%");
        foto = new Upload();
        foto.getStyle().set("box-sizing", "border-box");
        foto.getElement().appendChild(fotoPreview.getElement());
        nombre = new TextField("Nombre");
        activo = new Checkbox("Activo");
        costo = new TextField("Costo");
        descripcion = new TextField("Descripcion");
        formLayout.add(fotoLabel, foto, nombre, activo, costo, descripcion);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void attachImageUpload(Upload upload, Image preview) {
        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
        upload.setAcceptedFileTypes("image/*");
        upload.setReceiver((fileName, mimeType) -> {
            uploadBuffer.reset();
            return uploadBuffer;
        });
        upload.addSucceededListener(e -> {
            StreamResource resource = new StreamResource(e.getFileName(),
                    () -> new ByteArrayInputStream(uploadBuffer.toByteArray()));
            preview.setSrc(resource);
            preview.setVisible(true);
            if (this.servicio == null) {
                this.servicio = new Servicio();
            }
            this.servicio.setFoto(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Servicio value) {
        this.servicio = value;
        binder.readBean(this.servicio);
        this.fotoPreview.setVisible(value != null);
        if (value == null || value.getFoto() == null) {
            this.foto.clearFileList();
            this.fotoPreview.setSrc("");
        } else {
            this.fotoPreview.setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getFoto()));
        }

    }
}
