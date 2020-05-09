package io.zhudy.duic.repository

import io.r2dbc.spi.Row
import io.zhudy.duic.domain.App
import io.zhudy.duic.domain.AppContentHis
import io.zhudy.duic.domain.AppHis
import io.zhudy.duic.domain.AppPair
import io.zhudy.duic.vo.AppVo
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
abstract class AbstractAppRepository(
        private val dc: DatabaseClient
) : AppRepository {

    companion object {
        private const val INSERT_SQL = "INSERT INTO DUIC_APP(name,profile,description,token,ip_limit,content,v,gv,users,updated_at,created_at) VALUES(:name,:profile,:description,:token,:ipLimit,:content,:v,:gv,:users,NOW(),NOW())"
        private const val INSERT_HISTORY_SQL = "INSERT INTO DUIC_APP_HIS(name,profile,description,token,ip_limit,content,v,gv,users,updated_by,deleted_by,created_at) VALUES(:name,:profile,:description,:token,:ipLimit,:content,:v,:gv,:users,:updatedBy,:deletedBy,NOW())"
        private const val DELETE_SQL = "DELETE FROM DUIC_APP WHERE name=:name AND profile=:profile"
        private const val FIND_ONE_SQL = "SELECT * FROM DUIC_APP WHERE name=:name AND profile=:profile"
        private const val UPDATE_SQL = "UPDATE DUIC_APP SET token=:token,ip_limit=:ipLimit,description=:description,users=:users,updated_at=now() WHERE name=:name AND profile=:profile AND v=:v"
        private const val UPDATE_CONTENT_SQL = "UPDATE DUIC_APP SET content=:content,v=v+1,gv=:gv,updated_at=now() WHERE name=:name AND profile=:profile AND v=:v"
        private const val FIND_ALL_SQL = "SELECT * FROM DUIC_APP"
        private const val FIND_4_UPDATED_AT_SQL = "SELECT * FROM DUIC_APP WHERE updated_at>:time ORDER BY updated_at DESC"
        private const val FIND_APP_HISTORY_SQL = "SELECT * FROM DUIC_APP_HIS WHERE name=:name AND profile=:profile AND updated_by<>''"
        private const val FIND_ALL_NAMES = "SELECT name FROM DUIC_APP"
        private const val FIND_PROFILES_BY_NAME = "SELECT profile FROM DUIC_APP WHERE name=:name"
        private const val FIND_LATEST_DELETED = "SELECT * FROM DUIC_APP_HIS WHERE created_at>:time AND deleted_by<>''"
        private const val FIND_LAST_DATA_TIME_SQL = "SELECT updated_at FROM DUIC_APP ORDER BY updated_at DESC"
    }

    override fun insert(vo: AppVo.NewApp): Mono<Int> = nextGv().flatMap { gv ->
        dc.execute(INSERT_SQL)
                .bind("name", vo.name)
                .bind("profile", vo.profile)
                .bind("description", vo.description)
                .bind("token", vo.token)
                .bind("ipLimit", vo.ipLimit)
                .bind("content", vo.content)
                .bind("v", 1)
                .bind("gv", gv)
                .bind("users", vo.users.joinToString(","))
                .fetch()
                .rowsUpdated()
    }

    override fun insertHis(ah: AppHis): Mono<Int> = Mono.defer {
        dc.execute(INSERT_HISTORY_SQL)
                .bind("name", ah.name)
                .bind("profile", ah.profile)
                .bind("description", ah.description)
                .bind("token", ah.token)
                .bind("ipLimit", ah.ipLimit)
                .bind("content", ah.content)
                .bind("v", ah.v)
                .bind("gv", ah.gv)
                .bind("users", ah.users.joinToString(","))
                .bind("updated_by", ah.updatedBy)
                .bind("deleted_by", ah.deletedBy)
                .fetch()
                .rowsUpdated()
    }

    override fun delete(ap: AppPair): Mono<Int> = Mono.defer {
        dc.execute(DELETE_SQL)
                .bind("name", ap.name)
                .bind("profile", ap.profile)
                .fetch()
                .rowsUpdated()
    }

    override fun findOne(ap: AppPair): Mono<App> = Mono.defer {
        dc.execute(FIND_ONE_SQL)
                .bind("name", ap.name)
                .bind("profile", ap.profile)
                .map(::mapToApp)
                .one()
    }

    override fun update(ap: AppPair, vo: AppVo.UpdateBasicInfo): Mono<Int> = Mono.defer {
        dc.execute(UPDATE_SQL)
                .bind("token", vo.token)
                .bind("ipLimit", vo.ipLimit)
                .bind("description", vo.description)
                .bind("users", vo.users.joinToString(","))
                .bind("name", ap.name)
                .bind("profile", ap.profile)
                .bind("v", vo.v)
                .fetch()
                .rowsUpdated()
    }

    override fun updateContent(ap: AppPair, vo: AppVo.UpdateContent): Mono<Int> = nextGv().flatMap { gv ->
        dc.execute(UPDATE_CONTENT_SQL)
                .bind("name", ap.name)
                .bind("profile", ap.profile)
                .bind("v", vo.v)
                .bind("content", vo.content)
                .bind("gv", gv)
                .fetch()
                .rowsUpdated()
    }

    override fun findAll(): Flux<App> = Flux.defer {
        dc.execute(FIND_ALL_SQL)
                .map(::mapToApp)
                .all()
    }

    override fun find4UpdatedAt(time: LocalDateTime): Flux<App> = Flux.defer {
        dc.execute(FIND_4_UPDATED_AT_SQL)
                .bind("time", time)
                .map(::mapToApp)
                .all()
    }

    override fun findAppHistory(ap: AppPair, pageable: Pageable): Flux<AppContentHis> = Flux.defer {
        dc.execute(FIND_APP_HISTORY_SQL)
                .bind("name", ap.name)
                .bind("profile", ap.profile)
                .map(::mapToAppContentHis)
                .all()
    }

    override fun findAllNames(): Flux<String> = Flux.defer {
        dc.execute(FIND_ALL_NAMES)
                .map { row -> row.get(1, String::class.java) }
                .all()
    }

    override fun findProfilesByName(name: String): Flux<String> = Flux.defer {
        dc.execute(FIND_PROFILES_BY_NAME)
                .bind("name", name)
                .map { row -> row.get(1, String::class.java) }
                .all()
    }

    override fun findLatestDeleted(time: LocalDateTime): Flux<AppHis> = Flux.defer {
        dc.execute(FIND_LATEST_DELETED)
                .bind("time", time)
                .map(::mapToAppHis)
                .all()
    }

    override fun findLastDataTime(): Mono<Long> = Mono.defer {
        dc.execute(FIND_LAST_DATA_TIME_SQL)
                .map { row -> row.get(1, Long::class.java) }
                .first()
                .defaultIfEmpty(0)
    }

    protected fun mapToApp(row: Row) = App(
            id = row["id"] as Int,
            name = row["name"] as String,
            profile = row["profile"] as String,
            description = row["description"] as String,
            content = (row["content"] as? String) ?: "",
            token = row["token"] as? String,
            ipLimit = row["ip_limit"] as? String,
            v = row["v"] as Int,
            gv = row["gv"] as Long,
            users = (row["users"] as String).split(","),
            createdAt = row["created_at"] as LocalDateTime,
            updatedAt = row["updated_at"] as LocalDateTime
    )

    private fun mapToAppHis(row: Row) = AppHis(
            id = row["id"] as Int,
            name = row["name"] as String,
            profile = row["profile"] as String,
            description = row["description"] as String,
            content = row["content"] as String,
            token = row["token"] as String,
            ipLimit = row["ip_limit"] as String,
            v = row["v"] as Int,
            gv = row["gv"] as Long,
            updatedBy = row["updated_by"] as String,
            deletedBy = row["deleted_by"] as String,
            users = (row["users"] as String).split(","),
            createdAt = row["created_at"] as LocalDateTime
    )

    private fun mapToAppContentHis(row: Row) = AppContentHis(
            id = row["id"] as Int,
            content = row["content"] as String,
            updatedBy = row["updated_by"] as String,
            updatedAt = row["updated_at"] as LocalDateTime
    )
}