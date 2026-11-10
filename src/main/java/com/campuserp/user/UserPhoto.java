package com.campuserp.user;

import com.campuserp.common.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A user's profile photo, kept apart from User so logins never load the image. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_photo")
public class UserPhoto extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    // MySQL needs the length to pick a column big enough for 2 MB
    @Column(nullable = false, length = 3_000_000)
    private byte[] data;
}
