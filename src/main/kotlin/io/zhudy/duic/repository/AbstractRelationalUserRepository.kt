package io.zhudy.duic.repository

import io.zhudy.duic.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
abstract class AbstractRelationalUserRepository(
        private val dc: DatabaseClient
) : UserRepository {

    companion object {
        private const val INSERT_SQL = "INSERT INTO DUIC_USER(email,password,created_at,updated_at) VALUES(:email,:password,NOW(),NOW())"
        private const val DELETE_SQL = "DELETE FROM DUIC_USER WHERE email=:email"
        private const val UPDATE_PASSWORD_SQL = "UPDATE DUIC_USER SET password=:password WHERE email=:email"
        private const val FIND_BY_EMAIL_SQL = "SELECT * FROM DUIC_USER WHERE email=:email"
    }

    override fun insert(user: User): Mono<Int> = Mono.defer {
        dc.execute(INSERT_SQL)
                .bind("email", user.email)
                .bind("password", user.password)
                .fetch()
                .rowsUpdated()
    }

    override fun delete(email: String): Mono<Int> = Mono.defer {
        dc.execute(DELETE_SQL)
                .bind("email", email)
                .fetch()
                .rowsUpdated()
    }

    override fun updatePassword(email: String, password: String): Mono<Int> = Mono.defer {
        dc.execute(UPDATE_PASSWORD_SQL)
                .bind("email", email)
                .bind("password", password)
                .fetch()
                .rowsUpdated()
    }

    override fun findByEmail(email: String): Mono<User> = Mono.defer {
        dc.execute(FIND_BY_EMAIL_SQL)
                .bind("email", email)
                .`as`(User::class.java)
                .fetch()
                .one()
    }

    override fun findPage(pageable: Pageable): Mono<Page<User>> = Mono.defer {
        val content = dc.select()
                .from("DUIC_USER")
                .page(pageable)
                .`as`(User::class.java)
                .fetch()
                .all()

        val cm = dc.execute("SELECT COUNT(*) FROM DUIC_USER")
                .map { row -> row.get(0) as Long }
                .first()
                .defaultIfEmpty(0)

        content.collectList().zipWith(cm).map { t ->
            PageImpl(t.t1, pageable, t.t2)
        }
    }

    override fun findAllEmail(): Flux<String> = Flux.defer {
        dc.select()
                .from("DUIC_USER")
                .project("email")
                .map { row -> row.get(0, String::class.java) }
                .all()
    }
}