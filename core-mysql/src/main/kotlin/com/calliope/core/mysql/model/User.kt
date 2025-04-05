package com.calliope.core.mysql.model

import jakarta.persistence.*
import java.io.Serializable

/**
 * The persistent class for the genre database table.
 *
 */
@Entity
@Table(name = "user2")
@NamedQuery(name = "User.findAll", query = "SELECT u FROM User u")
class User  //	//bi-directional many-to-one association to Country
//	@ManyToOne
//	@JoinColumn(name="country_id")
//	private Country country;
//	//bi-directional many-to-one association to GenreStation
//	@OneToMany(mappedBy="user")
//	private List<UserStation> userStations;
    : Audit(), Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var userId: Int? = null

    @Column(nullable = false, unique = true, length = 255)
    var email: String? = null

    @Column(name = "salt_password_hash", nullable = false, length = 255)
    var saltPasswordHash: String? = null

    @Column(name = "given_name", length = 255)
    var givenName: String? = null

    @Column(name = "family_name", length = 255)
    var familyName: String? = null

    var ip: String? = null

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return userId == other.userId
    }

    override fun hashCode(): Int {
        return userId ?: 0
    }

}