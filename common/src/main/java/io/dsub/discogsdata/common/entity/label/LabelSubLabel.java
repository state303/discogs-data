package io.dsub.discogsdata.common.entity.label;

import javax.persistence.*;

@Entity
@Table(name = "label_sublabel")
public class LabelSubLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_label_id")
    private Label parent;

    @ManyToOne
    @JoinColumn(name = "sub_label_id")
    private Label subLabel;
}
