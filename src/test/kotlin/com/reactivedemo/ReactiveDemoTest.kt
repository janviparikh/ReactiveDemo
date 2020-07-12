package com.reactivedemo

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.util.logging.Logger


class ReactiveDemoTest {

    @Test
    fun nextSignal() {
        val helloMono = Mono.just("Hello")
            .log(Thread.currentThread().name)

        StepVerifier.create(helloMono)
            .consumeNextWith {
                it shouldBe "Hello"
            }
            .verifyComplete()
    }

    @Test
    fun completeSignal() {
        val helloMono = Mono.empty<String>()
            .log(Thread.currentThread().name)

        StepVerifier.create(helloMono)
            .verifyComplete()
    }

    @Test
    fun errorSignal() {
        val helloMono = Mono.error<String>(Throwable("Error in Hello"))
            .log(Thread.currentThread().name)

        StepVerifier.create(helloMono)
            .consumeErrorWith {
                it.message shouldBe "Error in Hello"
            }
            .verify()
    }

    @Test
    fun map() {
        val helloMono = Mono.just("Hello")
        val transformedMono = helloMono
            .map {
                it.toUpperCase()
            }
            .log(Thread.currentThread().name)

        StepVerifier.create(transformedMono)
            .consumeNextWith {
                it shouldBe "HELLO"
            }
            .verifyComplete()

        //synchronous function
    }

    @Test
    fun flatMap() {
        val linkedInProfileMono = getLinkedInProfile("janviparikh")
            .flatMap {
                saveToRepository(it)
            }
            .log(Thread.currentThread().name)

        StepVerifier.create(linkedInProfileMono)
            .consumeNextWith {
                it shouldBe "https://www.linkedin.com/in/janvi-parikh saved"
            }
            .verifyComplete()

        //asynchronous function
    }

    @Test
    fun chainedFlatmap() {
        val profiles =
            getLinkedInProfile("janviparikh")
                .flatMap {
                    saveToRepository(it)
                }
                .flatMap {
                    getGithubProfile("janviparikh")
                }
                .flatMap {
                    saveToRepository(it)
                }
                .log(Thread.currentThread().name)

        StepVerifier.create(profiles)
            .consumeNextWith {
                it shouldBe "https://github.com/janviparikh saved"
            }
            .verifyComplete()

        //One thing inside an operator
        //Stay away from nested maps & flatmaps
    }

    @Test
    fun zip() {
        val profileTupleMono =
            Mono.zip(getLinkedInProfile("janviparikh"), getGithubProfile("janviparikh"))
                .flatMap {
                    saveAllToRepository(it.t1, it.t2)
                }
                .log(Thread.currentThread().name)

        StepVerifier.create(profileTupleMono)
            .consumeNextWith {
                it shouldBe "https://www.linkedin.com/in/janvi-parikh & https://github.com/janviparikh saved"
            }
            .verifyComplete()

        //use zip for independent operations
    }

    @Test
    fun zipWithEmpty() {
        val profileTupleMono =
            Mono.zip(getLinkedInProfile("janviparikh"), getEmptyGithubProfile("janviparikh"))
                .flatMap {
                    saveAllToRepository(it.t1, it.t2)
                }
                .log(Thread.currentThread().name)

        StepVerifier.create(profileTupleMono)
            .consumeNextWith {
                it shouldBe "https://www.linkedin.com/in/janvi-parikh & https://github.com/janviparikh saved"
            }
            .verifyComplete()
    }

    @Test
    fun switchIfEmpty() {
        val profileTupleMono =
            Mono.zip(getLinkedInProfile("janviparikh")
                , getEmptyGithubProfile("janviparikh")
                .switchIfEmpty(getDefaultGithubProfile())
            )
                .flatMap {
                    saveAllToRepository(it.t1, it.t2)
                }
                .log(Thread.currentThread().name)

        StepVerifier.create(profileTupleMono)
            .consumeNextWith {
                it shouldBe "https://www.linkedin.com/in/janvi-parikh & https://github.com saved"
            }
            .verifyComplete()

        //Difference between switchIfEmpty() and switchIfEmpty{}
        //Eager evaluation
        //Can use Mono.defer to avoid eager  evaluation
    }

    @Test
    fun doOnError() {
        val logger = Logger.getLogger(ReactiveDemoTest::class.java.name)
        val errorGithubProfile = getErrorGithubProfile("janviparikh")
            .doOnError {
                //Typically recommended to use for side effects here, example log
                logger.info("Error while fetching githubProfile")
            }
            .log()
        //Similar doOnMethods

        StepVerifier.create(errorGithubProfile)
            .consumeErrorWith {
                it.message shouldBe "Github Not Reachable"
            }.verify()
    }

    @Test
    fun onErrorResume() {
        val githubProfile = getErrorGithubProfile("janviparikh")
            .onErrorResume {
                //Nothing stops you from logging here, conventionally side effects preferred in doOnMethods
                getDefaultGithubProfile()
            }
            .log()

        StepVerifier.create(githubProfile)
            .consumeNextWith {
                it shouldBe "https://github.com"
            }
            .verifyComplete()
    }

    @Test
    fun fromCallable() {
        //Sending email is non reactive and takes 5 secs
        //toMono Will block your thread
        val sendEmail = Mono.fromCallable { sendEmail("jerry@gmail.com") }.log()

        StepVerifier.create(sendEmail)
            .consumeNextWith {
                it shouldBe "email sent to jerry@gmail.com"
            }
            .verifyComplete()
    }

    @Test
    fun subscribeOn() {
        val sendEmailToTom = Mono.fromCallable { sendEmail("tom@gmail.com") }.log()
        val sendEmailToJerry = Mono.fromCallable { sendEmail("jerry@gmail.com") }.log()

        val email = Mono.zip(
            sendEmailToTom.subscribeOn(Schedulers.elastic()),
            sendEmailToJerry.subscribeOn(Schedulers.elastic()))

        StepVerifier.create(email)
            .consumeNextWith {
                it.t1 shouldBe "email sent to tom@gmail.com"
                it.t2 shouldBe "email sent to jerry@gmail.com"
            }
            .verifyComplete()
    }

    fun getLinkedInProfile(string: String): Mono<String> {
        val profileId = when (string) {
            "janviparikh" -> "janvi-parikh"
            else -> string
        }
        return Mono.just("https://www.linkedin.com/in/$profileId")
    }

    fun getGithubProfile(string: String): Mono<String> {
        return Mono.just("https://github.com/$string")
    }

    fun getEmptyGithubProfile(string: String): Mono<String> {
        return Mono.empty()
    }

    fun getErrorGithubProfile(string: String): Mono<String> {
        return Mono.error(Throwable("Github Not Reachable"))
    }

    fun getDefaultGithubProfile(): Mono<String> {
        println("Getting Default Profile")
        return Mono.just("https://github.com")
    }

    fun saveToRepository(string: String): Mono<String> {
        return Mono.just("$string saved")
    }

    fun saveAllToRepository(string1: String, string2: String): Mono<String> {
        return Mono.just("$string1 & $string2 saved")
    }

    fun sendEmail(email: String): String {
        Thread.sleep(5000)
        return "email sent to $email"
    }
}