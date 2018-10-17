package org.talend.sdk.component.marketplace.model;

import java.util.Collection;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.talend.sdk.component.marketplace.front.model.ComponentModel;
import org.talend.sdk.component.marketplace.service.ui.View;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(callSuper = true)
@NamedQueries({
        @NamedQuery(name = "Component.count", query = "select count(c) from Component c"),
        @NamedQuery(name = "Component.findAll", query = "select c from Component c order by c.name"),
        @NamedQuery(name = "Component.findAllWithProducts", query = "select c from Component c left join fetch c.products"),
        @NamedQuery(name = "Component.findAllByVendor", query = "select c from Component c where c.vendor = :vendor")
})
@Table(name = "components")
@View(ComponentModel.class)
public class Component extends AuditedEntity {
    @Column(length = 8192, nullable = false)
    private String description;

    @Column(length = 2048, nullable = false)
    private String changelog;

    @Column(length = 2048, nullable = false)
    private String license;

    @Column(length = 2048, nullable = false)
    private String sources;

    @Column(length = 2048, nullable = false)
    private String bugtracker;

    @Column(length = 2048, nullable = false)
    private String documentation;

    @ManyToOne
    @Column(length = 1024, nullable = false)
    private Vendor vendor;

    @OrderBy("updated DESC")
    @OneToMany(mappedBy = "component")
    private Collection<Download> downloads;

    @ManyToMany
    @OrderBy("name")
    @JoinTable(
            name = "components_products",
            joinColumns = @JoinColumn(name = "component_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "post_id", referencedColumnName = "id"))
    private List<Product> products;
}
